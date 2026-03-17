package com.mobile.watch

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.data.ExerciseUpdate
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

class ExerciseSessionService : Service(), MessageClient.OnMessageReceivedListener {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val healthClient by lazy { HealthServices.getClient(this) }
    private val exerciseClient by lazy { healthClient.exerciseClient }
    private val messageClient by lazy { Wearable.getMessageClient(this) }
    private val dataClient by lazy { Wearable.getDataClient(this) }
    private val nodeClient by lazy { Wearable.getNodeClient(this) }
    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }
    private val powerManager by lazy { getSystemService(PowerManager::class.java) }

    private var activeSessionId: String? = null
    private var nextSeq: Long = 1L
    private var lastSnapshotAt: Long = 0L
    private var lastMetrics: WatchMetrics? = null
    private val recentPackets = ArrayDeque<WatchSyncPacket>()

    private val exerciseConfig = ExerciseConfig(
        exerciseType = ExerciseType.RUNNING,
        dataTypes = setOf(
            DataType.HEART_RATE_BPM,
            DataType.SPEED,
            DataType.DISTANCE_TOTAL,
            DataType.CALORIES_TOTAL,
            DataType.STEPS_PER_MINUTE,
            DataType.ABSOLUTE_ELEVATION,
        ),
        isAutoPauseAndResumeEnabled = false,
        isGpsEnabled = false,
    )

    private val exerciseCallback = object : ExerciseUpdateCallback {
        override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
            serviceScope.launch {
                handleExerciseUpdate(update)
            }
        }

        override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) = Unit

        override fun onAvailabilityChanged(dataType: DataType<*, *>, availability: Availability) {
            Log.d(TAG, "[$dataType] availability changed: $availability")
        }

        override fun onRegistered() {
            Log.d(TAG, "Exercise callback registered")
        }

        override fun onRegistrationFailed(throwable: Throwable) {
            Log.e(TAG, "Exercise callback registration failed", throwable)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        exerciseClient.setUpdateCallback(exerciseCallback)
        messageClient.addListener(this)
            .addOnSuccessListener { Log.d(TAG, "Catch-up listener registered") }
            .addOnFailureListener { error -> Log.e(TAG, "Catch-up listener registration failed", error) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> serviceScope.launch { stopWorkoutSession() }
            else -> {
                promoteToForeground()
                serviceScope.launch { startWorkoutSession() }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        exerciseClient.clearUpdateCallbackAsync(exerciseCallback)
        messageClient.removeListener(this)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != WatchSyncPaths.catchupRequestPath) {
            return
        }

        serviceScope.launch {
            handleCatchupRequest(String(messageEvent.data))
        }
    }

    private suspend fun startWorkoutSession() {
        if (activeSessionId != null) {
            return
        }

        try {
            exerciseClient.startExerciseAsync(exerciseConfig).await()
            activeSessionId = UUID.randomUUID().toString()
            nextSeq = 1L
            lastSnapshotAt = 0L
            lastMetrics = null
            recentPackets.clear()
            Log.d(TAG, "Workout session started: $activeSessionId")
        } catch (error: Exception) {
            Log.e(TAG, "Failed to start workout session", error)
            stopSelf()
        }
    }

    private suspend fun stopWorkoutSession() {
        val sessionId = activeSessionId
        if (sessionId == null) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        try {
            val metrics = lastMetrics
            if (metrics != null) {
                val packet = nextPacket(sessionId, metrics, WatchPacketKind.SNAPSHOT)
                sendSnapshot(packet)
            }
            exerciseClient.endExerciseAsync().await()
            Log.d(TAG, "Workout session ended: $sessionId")
        } catch (error: Exception) {
            Log.e(TAG, "Failed to stop workout session", error)
        } finally {
            activeSessionId = null
            lastMetrics = null
            lastSnapshotAt = 0L
            recentPackets.clear()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private suspend fun handleExerciseUpdate(update: ExerciseUpdate) {
        val sessionId = activeSessionId ?: return
        val mergedMetrics = mergeMetrics(update) ?: run {
            Log.d(TAG, "No complete metrics yet, skipping update")
            return
        }
        lastMetrics = mergedMetrics

        val interactive = powerManager?.isInteractive == true
        val livePacket = nextPacket(sessionId, mergedMetrics, WatchPacketKind.LIVE)
        rememberPacket(livePacket)

        if (interactive) {
            sendMessagePacket(livePacket, WatchSyncPaths.livePath)
        }

        val shouldSendSnapshot =
            lastSnapshotAt == 0L || livePacket.capturedAt - lastSnapshotAt >= SNAPSHOT_INTERVAL_MS
        if (shouldSendSnapshot) {
            sendSnapshot(livePacket.withKind(WatchPacketKind.SNAPSHOT))
            lastSnapshotAt = livePacket.capturedAt
        }
    }

    private fun mergeMetrics(update: ExerciseUpdate): WatchMetrics? {
        val metrics = update.latestMetrics
        val previous = lastMetrics

        val heartRate = metrics.getData(DataType.HEART_RATE_BPM).lastOrNull()?.value?.toInt()
            ?: previous?.heartRate
            ?: return null

        val speedMps = metrics.getData(DataType.SPEED).lastOrNull()?.value ?: previous?.speedMps ?: 0.0
        val cadenceSpm = metrics.getData(DataType.STEPS_PER_MINUTE).lastOrNull()?.value?.toInt()
            ?: previous?.cadenceSpm
            ?: 0
        val altitudeM = metrics.getData(DataType.ABSOLUTE_ELEVATION).lastOrNull()?.value
            ?: previous?.altitudeM
            ?: 0.0
        val distanceM = metrics.getData(DataType.DISTANCE_TOTAL)?.total ?: previous?.distanceM ?: 0.0
        val caloriesKcal = metrics.getData(DataType.CALORIES_TOTAL)?.total ?: previous?.caloriesKcal ?: 0.0

        return WatchMetrics(
            heartRate = heartRate,
            speedMps = speedMps,
            distanceM = distanceM,
            caloriesKcal = caloriesKcal,
            altitudeM = altitudeM,
            cadenceSpm = cadenceSpm,
        )
    }

    private fun nextPacket(
        sessionId: String,
        metrics: WatchMetrics,
        kind: WatchPacketKind,
    ): WatchSyncPacket {
        return WatchSyncPacket(
            kind = kind,
            sessionId = sessionId,
            seq = nextSeq++,
            capturedAt = System.currentTimeMillis(),
            metrics = metrics,
        )
    }

    private fun rememberPacket(packet: WatchSyncPacket) {
        recentPackets.addLast(packet)
        while (recentPackets.size > MAX_RECENT_PACKETS) {
            recentPackets.removeFirst()
        }
    }

    private suspend fun sendMessagePacket(packet: WatchSyncPacket, path: String) {
        val nodes = nodeClient.connectedNodes.await()
        if (nodes.isEmpty()) {
            Log.w(TAG, "No connected nodes for path=$path seq=${packet.seq}")
            return
        }

        val payload = packet.toJsonString().toByteArray()
        nodes.forEach { node ->
            messageClient.sendMessage(node.id, path, payload).await()
            Log.d(TAG, "Sent $path seq=${packet.seq} to ${node.displayName}")
        }
    }

    private suspend fun sendSnapshot(packet: WatchSyncPacket) {
        val request = PutDataMapRequest.create(WatchSyncPaths.snapshotPath).apply {
            dataMap.putString(WatchSyncPaths.snapshotPayloadKey, packet.toJsonString())
            dataMap.putLong("seq", packet.seq)
            dataMap.putLong("capturedAt", packet.capturedAt)
        }.asPutDataRequest().setUrgent()

        dataClient.putDataItem(request).await()
        Log.d(TAG, "Snapshot synced seq=${packet.seq}")
    }

    private suspend fun handleCatchupRequest(rawRequest: String) {
        val lastSeenSeq = runCatching {
            JSONObject(rawRequest).optLong("lastSeenSeq", 0L)
        }.getOrDefault(0L)
        val packets = recentPackets.filter { it.seq > lastSeenSeq }
        if (packets.isEmpty()) {
            Log.d(TAG, "No catch-up packets for seq>$lastSeenSeq")
            return
        }

        packets.forEach { packet ->
            sendMessagePacket(packet.withKind(WatchPacketKind.CATCHUP), WatchSyncPaths.catchupPath)
        }
    }

    private fun promoteToForeground() {
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("운동 데이터 동기화")
            .setContentText("워치 데이터를 모바일로 전송 중입니다.")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .build()

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH,
        )
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Workout Sync",
            NotificationManager.IMPORTANCE_LOW,
        )
        notificationManager?.createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "ExerciseSessionService"
        private const val NOTIFICATION_CHANNEL_ID = "watch-sync-channel"
        private const val NOTIFICATION_ID = 1001
        private const val MAX_RECENT_PACKETS = 180
        private const val SNAPSHOT_INTERVAL_MS = 15_000L
        const val ACTION_START = "com.mobile.watch.action.START_WORKOUT"
        const val ACTION_STOP = "com.mobile.watch.action.STOP_WORKOUT"

        fun startIntent(context: Context): Intent {
            return Intent(context, ExerciseSessionService::class.java).setAction(ACTION_START)
        }

        fun stopIntent(context: Context): Intent {
            return Intent(context, ExerciseSessionService::class.java).setAction(ACTION_STOP)
        }
    }
}
