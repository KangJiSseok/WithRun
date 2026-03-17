package com.mobile.watch

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
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
        ambientObserver = AmbientLifecycleObserver(this, this)
        lifecycle.addObserver(ambientObserver)
        requestBatteryOptimizationExemption()
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
        // 포그라운드 서비스가 계속 실행 중이므로 별도 처리 불필요
    }

    // AOD 종료 (화면 활성화)
    override fun onExitAmbient() {
        isAmbient = false
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
                    // 실시간 운동 데이터
                    Text(text = "${data.heartRate} BPM", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text(text = "${String.format("%.1f", data.speedKmh)} km/h", fontSize = 14.sp)
                    Text(text = "${String.format("%.2f", data.distanceKm)} km", fontSize = 14.sp)
                    Text(text = "${data.caloriesKcal.toInt()} kcal", fontSize = 14.sp)
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
