package com.mobile.watch

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.PowerManager
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class ExerciseService : LifecycleService() {

    data class ExerciseData(
        val isActive: Boolean = false,
        val heartRate: Int = 0,
        val speedKmh: Double = 0.0,
        val distanceKm: Double = 0.0,
        val caloriesKcal: Double = 0.0,
        val altitudeM: Double = 0.0,
        val cadenceSpm: Int = 0,
    )

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP  = "ACTION_STOP"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "exercise_channel"
        private const val PREF_NAME = "exercise_prefs"
        private const val PREF_IS_ACTIVE = "is_active"

        private val _exerciseData = MutableStateFlow(ExerciseData())
        val exerciseData: StateFlow<ExerciseData> = _exerciseData.asStateFlow()
    }

    private val exerciseClient by lazy { HealthServices.getClient(this).exerciseClient }
    private val callbackExecutor = Executors.newSingleThreadExecutor()

    // 절전모드에서도 CPU를 유지해 센서 수집 속도가 느려지지 않도록 함
    private var wakeLock: PowerManager.WakeLock? = null

    // 1초 주기로 헬스 데이터를 전송하는 코루틴
    private var sendJob: Job? = null

    // isEnded 콜백이 연속으로 여러 번 발사될 때 중복 재시작 방지
    private val isRestartPending = AtomicBoolean(false)

    // 프로세스 재시작 후에도 측정 중 여부를 복구하기 위한 영속 저장소
    private val prefs by lazy { getSharedPreferences(PREF_NAME, MODE_PRIVATE) }

    private val exerciseConfig = ExerciseConfig(
        exerciseType = ExerciseType.RUNNING,
        dataTypes = setOf(
            DataType.HEART_RATE_BPM,
            DataType.SPEED,
            DataType.DISTANCE_TOTAL,
            DataType.CALORIES_TOTAL,
            DataType.STEPS_PER_MINUTE,
            // ABSOLUTE_ELEVATION 제거 — ACCESS_FINE_LOCATION 런타임 권한 요구하여 재시작 루프 시 SecurityException 유발
        ),
        isAutoPauseAndResumeEnabled = false,
        isGpsEnabled = false,   // GPS 없이 가속도계 기반 측정 (실내/실외 모두 안정적)
    )

    private val exerciseCallback = object : ExerciseUpdateCallback {

        override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
            val exerciseState = update.exerciseStateInfo.state
            Log.d("ExerciseService", "운동 상태: $exerciseState")

            // 운동이 자동 종료된 경우 (Samsung Health 세션 강제 종료, 비활동 감지 등)
            if (exerciseState.isEnded) {
                Log.w("ExerciseService", "운동 자동 종료 감지 — 사용자가 의도적으로 종료했는지 확인")
                // prefs=true: 사용자가 아직 측정 중 → 자동 재시작
                // prefs=false: 사용자가 직접 중지 → 정상 종료
                if (prefs.getBoolean(PREF_IS_ACTIVE, false)) {
                    // compareAndSet(false, true): 이미 재시작 중이면 스킵 — isEnded가 연속 수십 번 발사되는 것을 막음
                    if (isRestartPending.compareAndSet(false, true)) {
                        Log.w("ExerciseService", "비의도적 종료 감지 → 운동 자동 재시작 시도")
                        lifecycleScope.launch { startExercise() }
                    } else {
                        Log.d("ExerciseService", "재시작 이미 진행 중 — 중복 isEnded 콜백 무시")
                    }
                } else {
                    Log.d("ExerciseService", "의도적 종료 — 서비스 정리")
                    _exerciseData.value = _exerciseData.value.copy(isActive = false)
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
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
            val altitudeM    = metrics.getData(DataType.ABSOLUTE_ELEVATION).lastOrNull()?.value
                               ?: current.altitudeM
            val cadenceSpm   = metrics.getData(DataType.STEPS_PER_MINUTE).lastOrNull()?.value?.toInt()
                               ?: current.cadenceSpm

            Log.d("ExerciseService", "HR=$heartRate bpm, ${String.format("%.1f", speedMps * 3.6)} km/h, ${String.format("%.0f", distanceM)} m, $cadenceSpm spm")

            // 상태만 업데이트 — 실제 전송은 1초 주기 sendJob이 담당
            _exerciseData.value = ExerciseData(
                isActive     = true,
                heartRate    = heartRate,
                speedKmh     = speedMps * 3.6,
                distanceKm   = distanceM / 1000.0,
                caloriesKcal = caloriesKcal,
                altitudeM    = altitudeM,
                cadenceSpm   = cadenceSpm,
            )
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
        // [서비스 생명주기] onCreate 호출 = 서비스가 새로 생성됨 (이전에 없었거나 kill 후 재시작)
        Log.i("WR_Lifecycle", "ExerciseService.onCreate() — 서비스 새로 생성됨")
        createNotificationChannel()
        exerciseClient.setUpdateCallback(callbackExecutor, exerciseCallback)

        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WithRun:ExerciseWakeLock")
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
            null -> {
                // START_STICKY로 재시작된 경우
                // prefs로 측정 중 여부를 확인 (in-memory 상태는 프로세스 종료 시 초기화되므로 신뢰 불가)
                if (prefs.getBoolean(PREF_IS_ACTIVE, false)) {
                    Log.d("ExerciseService", "START_STICKY 재시작 → 운동 자동 재개")
                    startForeground(NOTIFICATION_ID, buildNotification())
                    lifecycleScope.launch { startExercise() }
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        // [서비스 생명주기] onDestroy = OS가 서비스를 종료함
        // 측정 중에 이 로그가 찍히면 → 서비스가 kill된 것 (버튼 초기화 원인)
        Log.w("WR_Lifecycle", "ExerciseService.onDestroy() — 서비스 종료됨, isActive=${_exerciseData.value.isActive}")
        sendJob?.cancel()
        if (wakeLock?.isHeld == true) wakeLock?.release()
        exerciseClient.clearUpdateCallbackAsync(exerciseCallback)
        super.onDestroy()
    }

    private suspend fun startExercise() {
        try {
            exerciseClient.startExerciseAsync(exerciseConfig).await()
            Log.d("ExerciseService", "운동 시작 성공")
            isRestartPending.set(false)
            _exerciseData.value = ExerciseData(isActive = true)
            prefs.edit().putBoolean(PREF_IS_ACTIVE, true).apply()
            Log.i("WR_WakeLock", "WakeLock 획득 시도 — isHeld=${wakeLock?.isHeld}")
            if (wakeLock?.isHeld == false) wakeLock?.acquire()
            Log.i("WR_WakeLock", "WakeLock 획득 완료 — isHeld=${wakeLock?.isHeld}")
            startSendLoop()
        } catch (e: Exception) {
            Log.e("ExerciseService", "운동 시작 실패", e)
            isRestartPending.set(false)
            _exerciseData.value = ExerciseData(isActive = false)
            prefs.edit().putBoolean(PREF_IS_ACTIVE, false).apply()
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
            sendJob?.cancel()
            if (wakeLock?.isHeld == true) wakeLock?.release()
            prefs.edit().putBoolean(PREF_IS_ACTIVE, false).apply()
            _exerciseData.value = ExerciseData(isActive = false)
        }
    }

    /**
     * 사용자가 측정을 종료할 때까지 1초마다 최신 헬스 데이터를 폰으로 전송.
     * Health Services 콜백 주기에 무관하게 일정한 간격을 보장.
     */
    private fun startSendLoop() {
        sendJob?.cancel()
        sendJob = lifecycleScope.launch {
            Log.i("WR_SendLoop", "sendLoop 시작")
            var loopCount = 0
            while (isActive) {
                val beforeMs = System.currentTimeMillis()
                val data = _exerciseData.value
                if (data.isActive) {
                    val speedMps = data.speedKmh / 3.6
                    val json = JSONObject().apply {
                        put("timestamp",    beforeMs)
                        put("heartRate",    data.heartRate)
                        put("speedMps",     speedMps)
                        put("distanceM",    data.distanceKm * 1000.0)
                        put("caloriesKcal", data.caloriesKcal)
                        put("altitudeM",    data.altitudeM)
                        put("cadenceSpm",   data.cadenceSpm)
                    }
                    sendToPhone(json.toString())
                }
                delay(1000L)
                loopCount++
                // [Q1 핵심] delay(1000) 후 실제 경과 시간 측정
                // 절전 시 이 값이 1000ms를 크게 초과하면 → Doze가 타이머를 늦추는 것
                val elapsedMs = System.currentTimeMillis() - beforeMs
                if (elapsedMs > 1500L) {
                    Log.w("WR_SendLoop", "[$loopCount] 전송 지연 감지! 경과=${elapsedMs}ms (정상=~1000ms) — Doze 또는 CPU 제한 의심")
                } else {
                    Log.d("WR_SendLoop", "[$loopCount] 전송 완료, 경과=${elapsedMs}ms")
                }
            }
            Log.i("WR_SendLoop", "sendLoop 종료 (코루틴 취소됨)")
        }
    }

    private suspend fun sendToPhone(json: String) {
        try {
            val nodes = Wearable.getNodeClient(this@ExerciseService).connectedNodes.await()
            if (nodes.isEmpty()) {
                Log.w("ExerciseService", "연결된 폰 없음 - 전송 스킵")
                return
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
