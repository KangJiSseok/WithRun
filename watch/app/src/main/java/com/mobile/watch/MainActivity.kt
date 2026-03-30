package com.mobile.watch

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.wear.ambient.AmbientLifecycleObserver
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

class MainActivity : ComponentActivity(), AmbientLifecycleObserver.AmbientLifecycleCallback {

    private lateinit var ambientObserver: AmbientLifecycleObserver
    private var isAmbient by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // [Q2 핵심] 이 로그가 절전/화면 꺼짐 후에 찍히면 → 앱이 완전히 재시작된 것 (버튼 초기화 원인)
        // 찍히지 않으면 → 앱은 살아있고 다른 원인으로 버튼이 초기화됨
        Log.i("WR_Lifecycle", "MainActivity.onCreate() — 앱 새로 생성됨")
        ambientObserver = AmbientLifecycleObserver(this, this)
        lifecycle.addObserver(ambientObserver)
        requestBatteryOptimizationExemption()
        promptUnrestrictedBattery()
        setContent {
            WatchApp(
                isAmbient = isAmbient,
                onStart = { startExerciseService() },
                onStop  = { stopExerciseService() },
            )
        }
    }

    // AOD 진입
    override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) {
        isAmbient = true
        // [Q1 참고] 이 시점 이후 WR_SendLoop 로그의 경과시간이 1500ms 초과하면 Doze 문제
        Log.i("WR_Lifecycle", "onEnterAmbient — 절전모드 진입, 서비스는 계속 실행 중이어야 함")
    }

    // AOD 종료 (화면 활성화)
    override fun onExitAmbient() {
        isAmbient = false
        Log.i("WR_Lifecycle", "onExitAmbient — 화면 복귀, isActive=${ExerciseService.exerciseData.value.isActive}")
    }

    // AOD 주기적 갱신 (1분마다 호출)
    override fun onUpdateAmbient() {}

    private fun startExerciseService() {
        startService(Intent(this, ExerciseService::class.java).apply {
            action = ExerciseService.ACTION_START
        })
    }

    private fun stopExerciseService() {
        startService(Intent(this, ExerciseService::class.java).apply {
            action = ExerciseService.ACTION_STOP
        })
    }

    // 삼성 워치의 공격적인 배터리 절약으로 인한 백그라운드 스로틀링 방지
    private fun requestBatteryOptimizationExemption() {
        val pm = getSystemService(PowerManager::class.java)
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            })
        }
    }

    // Samsung Health 콜백 throttling 완화를 위해 배터리 모드 "제한 없음" 설정 유도
    // 최초 1회만 표시 — 이후 사용자가 직접 설정한 것으로 간주
    private fun promptUnrestrictedBattery() {
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        if (prefs.getBoolean("battery_guide_shown", false)) return
        prefs.edit().putBoolean("battery_guide_shown", true).apply()

        android.app.AlertDialog.Builder(this)
            .setTitle("배터리 설정 권장")
            .setMessage(
                "운동 중 데이터가 끊기지 않으려면 두 곳에서\n" +
                "'배경 활동 허용'을 ON으로 설정해주세요.\n\n" +
                "① 이 앱 → 배터리 → 배경 활동 허용\n" +
                "② Samsung Health → 배터리 → 배경 활동 허용"
            )
            .setPositiveButton("이 앱 설정") { _, _ ->
                openAppSettings(packageName)
            }
            .setNeutralButton("Samsung Health 설정") { _, _ ->
                openAppSettings("com.samsung.android.wear.shealth")
            }
            .setNegativeButton("닫기", null)
            .show()
    }

    private fun openAppSettings(pkg: String) {
        try {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$pkg")
            })
        } catch (e: Exception) {
            Log.w("WR_Battery", "설정 화면 열기 실패: $pkg", e)
        }
    }

}

@Composable
fun WatchApp(
    isAmbient: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
) {
    val context = LocalContext.current
    val data by ExerciseService.exerciseData.collectAsState()
    val sync by WatchRunSyncState.state.collectAsState()
    val isRunning = data.isActive

    // 운동 중엔 화면이 꺼지지 않도록 유지 → 워치 페이스로 자동 이동 방지
    val activity = context as? Activity
    LaunchedEffect(isRunning) {
        if (isRunning) {
            activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it }) {
            onStart()
        } else {
            Toast.makeText(context, "권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    MaterialTheme {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            if (isAmbient) {
                // AOD 화면: 저전력 모드
                if (isRunning) {
                    Text(text = "${data.heartRate} BPM", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(text = "${data.caloriesKcal.toInt()} kcal", fontSize = 14.sp)
                } else {
                    Text(text = "대기 중")
                }
            } else {
                if (sync.isActive) {
                    WatchRunMiniMap(sync = sync)
                    val progressPct = (sync.routeProgressRatio * 100).toInt()
                    Text(
                        text = "진행 ${progressPct}%",
                        fontSize = 12.sp,
                        color = Color(0xFFD6FFE0),
                    )
                    if (sync.isOffCourse) {
                        Text(
                            text = "코스 이탈",
                            fontSize = 11.sp,
                            color = Color(0xFFFF6B6B),
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (isRunning) {
                    // 워치 전담 데이터: HR / 칼로리 (케이던스는 모바일 가속도계로 대체)
                    Text(text = "${data.heartRate} BPM", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(text = "${data.caloriesKcal.toInt()} kcal", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    Text(text = "대기 중")
                    Spacer(modifier = Modifier.height(10.dp))
                }

                val runningColor = Color(0xFFFF5A5A)
                val idleColor = Color(0xFF2EC47F)

                Box(
                    modifier = Modifier
                        .background(
                            color = Color(0x1FFFFFFF),
                            shape = RoundedCornerShape(999.dp),
                        )
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Button(
                        onClick = {
                            if (isRunning) {
                                onStop()
                            } else {
                                val required = arrayOf(
                                    Manifest.permission.BODY_SENSORS,
                                    Manifest.permission.ACTIVITY_RECOGNITION,
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                )
                                if (required.all {
                                        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                                    }) {
                                    onStart()
                                } else {
                                    permissionLauncher.launch(required)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (isRunning) runningColor else idleColor
                        ),
                        modifier = Modifier.size(width = 94.dp, height = 44.dp),
                    ) {
                        Text(
                            text = if (isRunning) "중지" else "시작",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WatchRunMiniMap(sync: WatchRunSyncData, size: Dp = 120.dp) {
    val points = remember(sync.courseCoordinates, sync.ghostCoordinates, sync.userTrail) {
        if (sync.courseCoordinates.isNotEmpty()) {
            sync.courseCoordinates
        } else {
            sync.ghostCoordinates + sync.userTrail
        }
    }
    if (points.isEmpty()) return

    val minLat = points.minOf { it.latitude }
    val maxLat = points.maxOf { it.latitude }
    val minLon = points.minOf { it.longitude }
    val maxLon = points.maxOf { it.longitude }

    Canvas(modifier = Modifier.size(size)) {
        val pad = 8f
        val width = this.size.width
        val height = this.size.height
        val lonRange = (maxLon - minLon).takeIf { it > 1e-9 } ?: 1e-9
        val latRange = (maxLat - minLat).takeIf { it > 1e-9 } ?: 1e-9

        fun toOffset(point: WatchPoint): Offset {
            val x = ((point.longitude - minLon) / lonRange).toFloat() * (width - 2 * pad) + pad
            val y = ((maxLat - point.latitude) / latRange).toFloat() * (height - 2 * pad) + pad
            return Offset(x, y)
        }

        fun drawPath(points: List<WatchPoint>, color: Color, stroke: Float) {
            if (points.size < 2) return
            var prev = toOffset(points.first())
            for (i in 1 until points.size) {
                val next = toOffset(points[i])
                drawLine(
                    color = color,
                    start = prev,
                    end = next,
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                prev = next
            }
        }

        // course polyline (항상 단일 라인만 노출)
        drawPath(sync.courseCoordinates, Color(0xFFFF6464), 4f)

        val ghostPoint = pointAtProgress(sync.ghostCoordinates, sync.ghostProgressRatio)
        ghostPoint?.let {
            val ghostColor = if (sync.mode == "GHOST") Color(0xFFF97316) else Color(0xFF66D9FF)
            drawCircle(color = ghostColor, radius = 4.5f, center = toOffset(it))
            drawCircle(
                color = Color.White,
                radius = 6f,
                center = toOffset(it),
                style = Stroke(width = 1.5f),
            )
        }

        sync.currentUserPoint?.let {
            val center = toOffset(it)
            drawCircle(color = Color(0xFF1E88E5), radius = 6f, center = center)
            drawCircle(
                color = Color.White,
                radius = 8f,
                center = center,
                style = Stroke(width = 2f),
            )

            if ((sync.speedMps > 0.5) && sync.headingDeg != null) {
                val rad = Math.toRadians(sync.headingDeg.toDouble())
                val dx = kotlin.math.sin(rad).toFloat() * 14f
                val dy = -kotlin.math.cos(rad).toFloat() * 14f
                drawLine(
                    color = Color(0xFF1E88E5),
                    start = center,
                    end = Offset(center.x + dx, center.y + dy),
                    strokeWidth = 3f,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

private fun pointAtProgress(points: List<WatchPoint>, ratio: Float): WatchPoint? {
    if (points.isEmpty()) return null
    if (points.size == 1) return points.first()

    val clamped = ratio.coerceIn(0f, 1f).toDouble()
    val cumulative = DoubleArray(points.size)
    for (i in 1 until points.size) {
        cumulative[i] = cumulative[i - 1] + haversineM(points[i - 1], points[i])
    }
    val total = cumulative.last().takeIf { it > 1e-6 } ?: return points.last()
    val target = total * clamped

    for (i in 0 until points.lastIndex) {
        val startDist = cumulative[i]
        val endDist = cumulative[i + 1]
        if (target < startDist || target > endDist) continue
        val segmentLen = endDist - startDist
        val t = if (segmentLen > 1e-6) (target - startDist) / segmentLen else 0.0
        return WatchPoint(
            latitude = points[i].latitude + (points[i + 1].latitude - points[i].latitude) * t,
            longitude = points[i].longitude + (points[i + 1].longitude - points[i].longitude) * t,
        )
    }

    return points.last()
}

private fun haversineM(a: WatchPoint, b: WatchPoint): Double {
    val r = 6_371_000.0
    val dLat = Math.toRadians(b.latitude - a.latitude)
    val dLon = Math.toRadians(b.longitude - a.longitude)
    val x = kotlin.math.sin(dLat / 2).let { it * it } +
        kotlin.math.cos(Math.toRadians(a.latitude)) *
        kotlin.math.cos(Math.toRadians(b.latitude)) *
        kotlin.math.sin(dLon / 2).let { it * it }
    return r * 2 * kotlin.math.atan2(kotlin.math.sqrt(x), kotlin.math.sqrt(1 - x))
}
