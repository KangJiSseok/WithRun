package com.mobile.watch

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.BatchingMode
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseInfo
import androidx.health.services.client.data.ExerciseTrackedStatus
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.data.ExerciseUpdate
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
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

    //서비스 전용 코루틴 스코프 (서비스에서 비동기 작업 돌리는 메인 엔진)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // (Health Services)
    private val healthClient by lazy { HealthServices.getClient(this) }

    //(Health Services)
    private val exerciseClient by lazy { healthClient.exerciseClient }

    //Wear OS 통신 (Data Layer)
    private val messageClient by lazy { Wearable.getMessageClient(this) }

    //Wear OS 통신 (Data Layer)
    private val dataClient by lazy { Wearable.getDataClient(this) }

    //Wear OS 통신 (Data Layer)
    private val nodeClient by lazy { Wearable.getNodeClient(this) }

    //시스템 서비스 (포그라운드 서비스 유지용 (필수))
    private val notificationManager by lazy { getSystemService(NotificationManager::class.java) }

    //화면 상태 확인 (화면 켜짐/꺼짐 판단)
    private val powerManager by lazy { getSystemService(PowerManager::class.java) }

    //상태 저장소 (세션 상태 저장) {앱 죽어도 이어서 하려고}
    private val sessionStateStore by lazy { WatchSessionStateStore(applicationContext) }

    private var activeSessionId: String? = null
    private var nextSeq: Long = 1L
    private var lastSnapshotAt: Long = 0L
    private var lastMetrics: WatchMetrics? = null
    private var lastInteractiveState: Boolean? = null
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
        Log.d(TAG, "Service created")
        restoreSessionState()
        createNotificationChannel()
        exerciseClient.setUpdateCallback(exerciseCallback)
        messageClient.addListener(this)
            .addOnSuccessListener { Log.d(TAG, "Catch-up listener registered") }
            .addOnFailureListener { error -> Log.e(TAG, "Catch-up listener registration failed", error) }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand action=${intent?.action} startId=$startId")
        when {
            intent?.action == ACTION_STOP -> serviceScope.launch { stopWorkoutSession() }
            WatchServiceLifecyclePolicy.shouldStartWorkout(intent?.action) -> {
                promoteToForeground()
                serviceScope.launch { startWorkoutSession() }
            }
            WatchServiceLifecyclePolicy.shouldRecoverService(intent?.action) -> {
                serviceScope.launch { recoverWorkoutSessionIfNeeded() }
            }
            else -> Log.w(TAG, "Ignoring unsupported action=${intent?.action}")
        }
        return START_STICKY
    }

    override fun onDestroy() {
        Log.w(TAG, "Service destroyed activeSessionId=$activeSessionId nextSeq=$nextSeq")
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
        val exerciseInfo = getCurrentExerciseInfo()
        if (exerciseInfo != null &&
            WatchServiceLifecyclePolicy.shouldAttemptStop(exerciseInfo.exerciseTrackedStatus)
        ) {
            if (activeSessionId == null) {
                activeSessionId = sessionStateStore.load()?.sessionId ?: UUID.randomUUID().toString()
            }
            Log.d(TAG, "Workout already active in Health Services: $activeSessionId")
            promoteToForeground()
            applyBatchingModeOverridesIfSupported()
            return
        }

        if (activeSessionId != null) {
            Log.d(TAG, "Clearing stale in-memory session before fresh start: $activeSessionId")
            clearSessionState(clearPersisted = true)
        }

        try {
            exerciseClient.startExerciseAsync(exerciseConfig).await()
            activeSessionId = UUID.randomUUID().toString()
            nextSeq = 1L
            lastSnapshotAt = 0L
            lastMetrics = null
            recentPackets.clear()
            persistSessionState()
            applyBatchingModeOverridesIfSupported()
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
            val exerciseInfo = getCurrentExerciseInfo()
            if (exerciseInfo == null) {
                Log.w(TAG, "Current exercise info unavailable while stopping; clearing local state")
            } else if (WatchServiceLifecyclePolicy.shouldAttemptStop(exerciseInfo.exerciseTrackedStatus)) {
                exerciseClient.endExerciseAsync().await()
                Log.d(TAG, "Workout session ended: $sessionId")
            } else {
                Log.w(
                    TAG,
                    "Skipping endExerciseAsync because trackedStatus=${exerciseInfo.exerciseTrackedStatus}",
                )
            }
        } catch (error: Exception) {
            Log.e(TAG, "Failed to stop workout session", error)
        } finally {
            clearSessionState(clearPersisted = true)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private suspend fun recoverWorkoutSessionIfNeeded() {
        val exerciseInfo = getCurrentExerciseInfo()
        if (exerciseInfo == null) {
            Log.w(TAG, "Unable to recover service because current exercise info is unavailable")
            return
        }

        if (!WatchServiceLifecyclePolicy.shouldAttemptStop(exerciseInfo.exerciseTrackedStatus)) {
            Log.d(TAG, "No owned active exercise to recover; clearing stale state")
            clearSessionState(clearPersisted = true)
            stopSelf()
            return
        }

        if (activeSessionId == null) {
            activeSessionId = sessionStateStore.load()?.sessionId ?: UUID.randomUUID().toString()
        }
        promoteToForeground()
        applyBatchingModeOverridesIfSupported()
        Log.d(TAG, "Recovered active workout session: $activeSessionId")
    }

    private suspend fun handleExerciseUpdate(update: ExerciseUpdate) {
        Log.d(TAG, "Exercise update received activeSessionId=$activeSessionId")
        val sessionId = activeSessionId ?: run {
            Log.w(TAG, "Dropping exercise update because activeSessionId is null")
            return
        }
        val mergedMetrics = mergeMetrics(update) ?: run {
            Log.d(TAG, "No complete metrics yet, skipping update")
            return
        }
        lastMetrics = mergedMetrics

        val interactive = powerManager?.isInteractive == true
        if (lastInteractiveState != interactive) {
            lastInteractiveState = interactive
            Log.d(TAG, "Power state changed interactive=$interactive activeSessionId=$activeSessionId")
            if (!interactive) {
                logMissingBackgroundSensorPermissionIfNeeded()
            }
        }
        val livePacket = nextPacket(sessionId, mergedMetrics, WatchPacketKind.LIVE)
        rememberPacket(livePacket)

        if (WatchDeliveryPolicy.shouldSendLivePacket(hasActiveWorkout = true, isInteractive = interactive)) {
            sendMessagePacket(livePacket, WatchSyncPaths.livePath)
        }

        if (WatchDeliveryPolicy.shouldUseDurableSync(isInteractive = interactive)) {
            syncDataItemPacket(livePacket)
            lastSnapshotAt = livePacket.capturedAt
            persistSessionState()
            return
        }

        val shouldSendSnapshot =
            lastSnapshotAt == 0L || livePacket.capturedAt - lastSnapshotAt >= SNAPSHOT_INTERVAL_MS
        if (shouldSendSnapshot) {
            sendSnapshot(livePacket.withKind(WatchPacketKind.SNAPSHOT))
            lastSnapshotAt = livePacket.capturedAt
            persistSessionState()
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
        val packet = WatchSyncPacket(
            kind = kind,
            sessionId = sessionId,
            seq = nextSeq++,
            capturedAt = System.currentTimeMillis(),
            metrics = metrics,
        )
        persistSessionState()
        return packet
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

    private suspend fun syncDataItemPacket(packet: WatchSyncPacket) {
        val request = PutDataMapRequest.create(WatchSyncPaths.snapshotPath).apply {
            dataMap.putString(WatchSyncPaths.snapshotPayloadKey, packet.toJsonString())
            dataMap.putLong("seq", packet.seq)
            dataMap.putLong("capturedAt", packet.capturedAt)
            dataMap.putString("kind", packet.kind.value)
        }.asPutDataRequest().setUrgent()

        dataClient.putDataItem(request).await()
        Log.d(TAG, "Durable data sync seq=${packet.seq} kind=${packet.kind.value}")
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
        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("운동 데이터 동기화")
            .setContentText("워치 데이터를 모바일로 전송 중입니다.")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setContentIntent(createMainActivityPendingIntent())

        OngoingActivity.Builder(applicationContext, NOTIFICATION_ID, notificationBuilder)
            .setTouchIntent(createMainActivityPendingIntent())
            .setStatus(Status.Builder().addTemplate("운동 측정 중").build())
            .build()
            .apply(applicationContext)

        val notification = notificationBuilder.build()

        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH,
        )
    }

    private fun createMainActivityPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
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

    private fun logMissingBackgroundSensorPermissionIfNeeded() {
        val permission = WatchPermissionPolicy.backgroundSensorPermission(Build.VERSION.SDK_INT) ?: return
        val granted = ContextCompat.checkSelfPermission(this, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (!granted) {
            Log.w(TAG, "Missing $permission; AOD/background sensor delivery may stop")
        }
    }

    private fun restoreSessionState() {
        val state = sessionStateStore.load() ?: run {
            Log.d(TAG, "No persisted session state to restore")
            return
        }

        activeSessionId = state.sessionId
        nextSeq = state.nextSeq
        lastSnapshotAt = state.lastSnapshotAt
        Log.d(
            TAG,
            "Restored session state sessionId=${state.sessionId} nextSeq=${state.nextSeq} lastSnapshotAt=${state.lastSnapshotAt}",
        )
    }

    private fun persistSessionState() {
        val sessionId = activeSessionId ?: return
        sessionStateStore.save(
            WatchSessionState(
                sessionId = sessionId,
                nextSeq = nextSeq,
                lastSnapshotAt = lastSnapshotAt,
            ),
        )
    }

    private suspend fun getCurrentExerciseInfo(): ExerciseInfo? {
        return runCatching {
            exerciseClient.getCurrentExerciseInfoAsync().await().also { info ->
                Log.d(
                    TAG,
                    "Current exercise trackedStatus=${info.exerciseTrackedStatus} type=${info.exerciseType}",
                )
            }
        }.getOrElse { error ->
            Log.w(TAG, "Failed to get current exercise info: ${error.message}")
            null
        }
    }

    private fun clearSessionState(clearPersisted: Boolean) {
        activeSessionId = null
        lastMetrics = null
        lastSnapshotAt = 0L
        recentPackets.clear()
        if (clearPersisted) {
            sessionStateStore.clear()
        }
    }

    private suspend fun applyBatchingModeOverridesIfSupported() {
        val batchingModes = resolveBatchingModeOverrides()
        if (batchingModes.isEmpty()) {
            Log.d(TAG, "No supported batching overrides for active exercise")
            return
        }

        runCatching {
            exerciseClient.overrideBatchingModesForActiveExerciseAsync(batchingModes).await()
            Log.d(TAG, "Applied batching overrides: $batchingModes")
        }.onFailure { error ->
            Log.w(TAG, "Failed to apply batching overrides: ${error.message}")
        }
    }

    private suspend fun resolveBatchingModeOverrides(): Set<BatchingMode> {
        return runCatching {
            val capabilities = exerciseClient.getCapabilitiesAsync().await()
            val batchingModes = WatchDeliveryPolicy.selectBatchingModeOverrides(
                supportedModes = capabilities.supportedBatchingModeOverrides,
            )
            Log.d(TAG, "Supported batching overrides: ${capabilities.supportedBatchingModeOverrides}")
            batchingModes
        }.getOrElse { error ->
            Log.w(TAG, "Failed to resolve batching mode overrides: ${error.message}")
            emptySet()
        }
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
