package com.mobile.watch

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.*
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.util.concurrent.Executors

class ExerciseService : LifecycleService() {

    data class ExerciseData(
        val isActive: Boolean = false,
        val heartRate: Int = 0,
        val speedKmh: Double = 0.0,
        val distanceKm: Double = 0.0,
        val caloriesKcal: Double = 0.0,
        val cadenceSpm: Int = 0,
    )

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP  = "ACTION_STOP"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "exercise_channel"

        private val _exerciseData = MutableStateFlow(ExerciseData())
        val exerciseData: StateFlow<ExerciseData> = _exerciseData.asStateFlow()
    }

    private val exerciseClient by lazy { HealthServices.getClient(this).exerciseClient }
    private val callbackExecutor = Executors.newSingleThreadExecutor()

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
        isGpsEnabled = false,   // GPS 없이 가속도계 기반 측정 (실내/실외 모두 안정적)
    )

    private val exerciseCallback = object : ExerciseUpdateCallback {

        override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
            val exerciseState = update.exerciseStateInfo.state
            Log.d("ExerciseService", "운동 상태: $exerciseState")

            // 운동이 자동 종료된 경우 (배터리 절약, 비활동 감지 등)
            if (exerciseState.isEnded) {
                Log.w("ExerciseService", "운동 자동 종료 감지 → 서비스 정리")
                _exerciseData.value = _exerciseData.value.copy(isActive = false)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return
            }

            val metrics = update.latestMetrics
            val current = _exerciseData.value

            // Health Services는 업데이트마다 모든 메트릭을 보내지 않음
            // → 이번 업데이트에 없는 메트릭은 이전 값 유지
            val heartRate    = metrics.getData(DataType.HEART_RATE_BPM).lastOrNull()?.value?.toInt()
                               ?: current.heartRate
            val speedMps     = metrics.getData(DataType.SPEED).lastOrNull()?.value
                               ?: (current.speedKmh / 3.6)
            val distanceM    = metrics.getData(DataType.DISTANCE_TOTAL)?.total
                               ?: (current.distanceKm * 1000.0)
            val caloriesKcal = metrics.getData(DataType.CALORIES_TOTAL)?.total
                               ?: current.caloriesKcal
            val altitudeM    = metrics.getData(DataType.ABSOLUTE_ELEVATION).lastOrNull()?.value ?: 0.0
            val cadenceSpm   = metrics.getData(DataType.STEPS_PER_MINUTE).lastOrNull()?.value?.toInt()
                               ?: current.cadenceSpm

            Log.d("ExerciseService", "HR=$heartRate bpm, ${String.format("%.1f", speedMps * 3.6)} km/h, ${String.format("%.0f", distanceM)} m, $cadenceSpm spm")

            _exerciseData.value = ExerciseData(
                isActive     = true,
                heartRate    = heartRate,
                speedKmh     = speedMps * 3.6,
                distanceKm   = distanceM / 1000.0,
                caloriesKcal = caloriesKcal,
                cadenceSpm   = cadenceSpm,
            )

            if (heartRate > 0) {
                val json = JSONObject().apply {
                    put("timestamp",    System.currentTimeMillis())
                    put("heartRate",    heartRate)
                    put("speedMps",     speedMps)
                    put("distanceM",    distanceM)
                    put("caloriesKcal", caloriesKcal)
                    put("altitudeM",    altitudeM)
                    put("cadenceSpm",   cadenceSpm)
                }
                sendToPhone(json.toString())
            }
        }

        override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) {}

        override fun onAvailabilityChanged(dataType: DataType<*, *>, availability: Availability) {
            Log.d("ExerciseService", "[$dataType] 가용성 변경: $availability")
        }

        override fun onRegistered() {
            Log.d("ExerciseService", "ExerciseCallback 등록 완료")
        }

        override fun onRegistrationFailed(throwable: Throwable) {
            Log.e("ExerciseService", "ExerciseCallback 등록 실패", throwable)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        exerciseClient.setUpdateCallback(callbackExecutor, exerciseCallback)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, buildNotification())
                lifecycleScope.launch { startExercise() }
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                lifecycleScope.launch {
                    stopExercise()
                    stopSelf()
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        exerciseClient.clearUpdateCallbackAsync(exerciseCallback)
        super.onDestroy()
    }

    private suspend fun startExercise() {
        try {
            exerciseClient.startExerciseAsync(exerciseConfig).await()
            Log.d("ExerciseService", "운동 시작 성공")
            _exerciseData.value = ExerciseData(isActive = true)
        } catch (e: Exception) {
            Log.e("ExerciseService", "운동 시작 실패", e)
            _exerciseData.value = ExerciseData(isActive = false)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private suspend fun stopExercise() {
        try {
            exerciseClient.endExerciseAsync().await()
            Log.d("ExerciseService", "운동 종료")
        } catch (e: Exception) {
            Log.e("ExerciseService", "운동 종료 실패", e)
        } finally {
            _exerciseData.value = ExerciseData(isActive = false)
        }
    }

    private fun sendToPhone(json: String) {
        lifecycleScope.launch {
            try {
                val nodes = Wearable.getNodeClient(this@ExerciseService).connectedNodes.await()
                if (nodes.isEmpty()) {
                    Log.w("ExerciseService", "연결된 폰 없음 - 전송 스킵")
                    return@launch
                }
                nodes.forEach { node ->
                    Wearable.getMessageClient(this@ExerciseService)
                        .sendMessage(node.id, "/health-data", json.toByteArray())
                        .await()
                    Log.d("ExerciseService", "전송 완료 → ${node.displayName}")
                }
            } catch (e: Exception) {
                Log.e("ExerciseService", "전송 실패", e)
            }
        }
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(CHANNEL_ID, "운동 추적", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        // 탭하면 운동 화면으로 복귀
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
            PendingIntent.FLAG_IMMUTABLE,
        )

        val notifBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("운동 중")
            .setContentText("건강 데이터를 수집하고 있습니다")
            .setOngoing(true)

        // OngoingActivity: 워치페이스에 운동 중 표시 +
        // Samsung Health 플랫폼에 "진행 중인 운동" 시그널 → 세션 자동 종료 방지
        val ongoingActivity = OngoingActivity.Builder(applicationContext, NOTIFICATION_ID, notifBuilder)
            .setTouchIntent(pendingIntent)
            .setStatus(Status.Builder().addTemplate("운동 중").build())
            .build()
        ongoingActivity.apply(applicationContext)

        return notifBuilder.build()
    }
}
