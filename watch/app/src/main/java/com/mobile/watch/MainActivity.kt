package com.mobile.watch

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.wear.ambient.AmbientLifecycleObserver
import androidx.wear.compose.material.Button
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
fun WatchApp(isAmbient: Boolean, onStart: () -> Unit, onStop: () -> Unit) {
    val context = LocalContext.current
    val data by ExerciseService.exerciseData.collectAsState()
    val isRunning = data.isActive

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
                    Text(text = "${String.format("%.2f", data.distanceKm)} km", fontSize = 14.sp)
                } else {
                    Text(text = "대기 중")
                }
            } else {
                if (isRunning) {
                    // 워치 전담 데이터: HR / 칼로리 / 케이던스
                    // 속도·거리는 모바일 GPS가 담당
                    Text(text = "${data.heartRate} BPM", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(text = "${data.caloriesKcal.toInt()} kcal", fontSize = 14.sp)
                    Text(text = "${data.cadenceSpm} spm", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                } else {
                    Text(text = "대기 중")
                    Spacer(modifier = Modifier.height(10.dp))
                }
                Button(onClick = {
                    if (isRunning) {
                        onStop()
                    } else {
                        val required = arrayOf(
                            Manifest.permission.BODY_SENSORS,
                            Manifest.permission.ACTIVITY_RECOGNITION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                        )
                        if (required.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
                            onStart()
                        } else {
                            permissionLauncher.launch(required)
                        }
                    }
                }) {
                    Text(text = if (isRunning) "중지" else "시작")
                }
            }
        }
    }
}
