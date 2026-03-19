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
import java.util.concurrent.atomic.AtomicInteger

class ExerciseService : LifecycleService() {

    // 워치 전담 데이터
    // HR: Samsung Health PPG + 모션보정 (모바일 불가)
    // 칼로리: HR 기반 개인화 누적값
    // 케이던스: Samsung Health 만보계 기반 SPM (모바일 가속도계보다 정확)
    // 거리: 만보계 기반 누적거리 (GPS 없이 워치 내부 추정)
    data class ExerciseData(
        val isActive: Boolean = false,
        val heartRate: Int = 0,
        val caloriesKcal: Double = 0.0,
        val cadenceSpm: Int = 0,
        val distanceM: Double = 0.0,
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
    // 빠른 연속 재시작 횟수 추적 (Samsung Health가 세션을 반복 거부하는 경우 포기 처리)
    private val consecutiveRestarts = AtomicInteger(0)

    // 프로세스 재시작 후에도 측정 중 여부를 복구하기 위한 영속 저장소
    private val prefs by lazy { getSharedPreferences(PREF_NAME, MODE_PRIVATE) }

    private val exerciseConfig = ExerciseConfig(
        exerciseType = ExerciseType.RUNNING,
        dataTypes = setOf(
            DataType.HEART_RATE_BPM,        // PPG + 모션보정 심박수
            DataType.CALORIES_TOTAL,        // HR 기반 누적 칼로리
            DataType.STEPS_PER_MINUTE,      // 삼성 헬스 케이던스 (모바일 가속도계보다 정확)
            DataType.DISTANCE_TOTAL,        // 만보계 기반 누적 거리
        ),
        isAutoPauseAndResumeEnabled = false,
        isGpsEnabled = false,
    )

    private val exerciseCallback = object : ExerciseUpdateCallback {

        override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
            lastMetricsAtMs = System.currentTimeMillis()
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
                        val attempts = consecutiveRestarts.incrementAndGet()
                        // 지수 백오프: 1회=즉시, 2회=5초, 3회=15초, 4회 이후=60초
                        val delayMs = when (attempts) {
                            1 -> 0L
                            2 -> 5_000L
                            3 -> 15_000L
                            else -> 60_000L
                        }
                        Log.w("ExerciseService", "비의도적 종료 → 재시작 ${attempts}회 (${delayMs / 1000}초 후)")
                        lifecycleScope.launch {
                            if (delayMs > 0) delay(delayMs)
                            startExercise()
                        }
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
            val caloriesKcal = metrics.getData(DataType.CALORIES_TOTAL)?.total
                               ?: current.caloriesKcal
            val cadenceSpm   = metrics.getData(DataType.STEPS_PER_MINUTE).lastOrNull()?.value?.toInt()
                               ?: current.cadenceSpm
            val distanceM    = metrics.getData(DataType.DISTANCE_TOTAL)?.total
                               ?: current.distanceM

            Log.d("ExerciseService", "HR=$heartRate bpm, ${caloriesKcal.toInt()} kcal, cadence=$cadenceSpm spm, dist=${distanceM.toInt()} m")

            _exerciseData.value = _exerciseData.value.copy(
                isActive     = true,
                heartRate    = if (heartRate > 0) heartRate else current.heartRate,
                caloriesKcal = caloriesKcal,
                cadenceSpm   = cadenceSpm,
                distanceM    = distanceM,
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

    @Volatile
    private var lastMetricsAtMs: Long = 0L

    override fun onCreate() {
        super.onCreate()
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
            _exerciseData.value = _exerciseData.value.copy(isActive = true)
            prefs.edit().putBoolean(PREF_IS_ACTIVE, true).apply()
            Log.i("WR_WakeLock", "WakeLock 획득 시도 — isHeld=${wakeLock?.isHeld}")
            if (wakeLock?.isHeld == false) wakeLock?.acquire()
            Log.i("WR_WakeLock", "WakeLock 획득 완료 — isHeld=${wakeLock?.isHeld}")
            startSendLoop()
            lifecycleScope.launch {
                delay(30_000L)
                if (_exerciseData.value.isActive) {
                    consecutiveRestarts.set(0)
                    Log.d("ExerciseService", "30초 안정 운동 확인 → 재시작 카운터 초기화")
                }
            }
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
     * 1초마다 HR/칼로리/케이던스/거리를 폰으로 전송.
     * 속도/위치는 모바일 GPS가 담당하므로 전송하지 않음.
     */
    private fun startSendLoop() {
        sendJob?.cancel()
        sendJob = lifecycleScope.launch {
            Log.i("WR_SendLoop", "sendLoop 시작")
            var loopCount = 0
            while (isActive) {
                val beforeMs = System.currentTimeMillis()
                val data = _exerciseData.value
                val staleMs = beforeMs - lastMetricsAtMs
                if (data.isActive && lastMetricsAtMs > 0L && staleMs <= 2_000L) {
                    val json = JSONObject().apply {
                        put("timestamp",    beforeMs)
                        put("heartRate",    data.heartRate)
                        put("caloriesKcal", data.caloriesKcal)
                        put("cadenceSpm",   data.cadenceSpm)
                        put("distanceM",    data.distanceM)
                    }
                    Log.d("WR_SendLoop", "[$loopCount] HR=${data.heartRate} bpm, cadence=${data.cadenceSpm} spm, dist=${data.distanceM.toInt()} m")
                    sendToPhone(json.toString())
                } else if (data.isActive) {
                    Log.w("WR_SendLoop", "[$loopCount] 최신 데이터 정지 감지 — 전송 스킵 (stale=${staleMs}ms)")
                }
                delay(1000L)
                loopCount++
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

        val ongoingActivity = OngoingActivity.Builder(applicationContext, NOTIFICATION_ID, notifBuilder)
            .setTouchIntent(pendingIntent)
            .setStatus(Status.Builder().addTemplate("운동 중").build())
            .build()
        ongoingActivity.apply(applicationContext)

        return notifBuilder.build()
    }
}
