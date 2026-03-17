package com.mobile.watch

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
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
import androidx.compose.ui.unit.dp
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
        setContent {
            WatchApp(
                isAmbient = isAmbient,
                onStart = { startExercise() },
                onStop  = { stopExercise() },
            )
        }
    }

    // AOD 진입
    override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) {
        isAmbient = true
        Log.d("WatchApp", "AOD 진입")
    }

    // AOD 종료 (화면 활성화)
    override fun onExitAmbient() {
        isAmbient = false
        Log.d("WatchApp", "AOD 종료")
    }

    // AOD 주기적 갱신 (1분마다 호출)
    override fun onUpdateAmbient() {
        Log.d("WatchApp", "AOD 갱신")
    }

    private fun startExercise() {
        Log.d("WatchApp", "운동 서비스 시작 요청")
        ContextCompat.startForegroundService(this, ExerciseSessionService.startIntent(this))
        showToast("운동 시작")
    }

    private fun stopExercise() {
        startService(ExerciseSessionService.stopIntent(this))
        showToast("운동 종료")
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun WatchApp(isAmbient: Boolean, onStart: () -> Unit, onStop: () -> Unit) {
    val context = LocalContext.current
    var isRunning by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        if (grants.values.all { it }) {
            isRunning = true
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
                // AOD 화면: 저전력 모드, 흰색 텍스트만 표시
                Text(text = if (isRunning) "측정 중..." else "대기 중")
            } else {
                // 일반 화면
                Text(text = if (isRunning) "측정 중..." else "대기 중")
                Spacer(modifier = Modifier.height(10.dp))
                Button(onClick = {
                    if (isRunning) {
                        isRunning = false
                        onStop()
                    } else {
                        val required = arrayOf(
                            Manifest.permission.BODY_SENSORS,
                            Manifest.permission.ACTIVITY_RECOGNITION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                        )
                        if (required.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
                            isRunning = true
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
