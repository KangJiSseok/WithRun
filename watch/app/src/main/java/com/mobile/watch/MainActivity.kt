package com.mobile.watch

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.wear.ambient.AmbientLifecycleObserver
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

class MainActivity : ComponentActivity(), AmbientLifecycleObserver.AmbientLifecycleCallback {

    private lateinit var ambientObserver: AmbientLifecycleObserver
    private val powerManager by lazy { getSystemService(PowerManager::class.java) }
    private var isAmbient by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ambientObserver = AmbientLifecycleObserver(this, this)
        lifecycle.addObserver(ambientObserver)
        logLifecycle("onCreate")
        setContent {
            WatchApp(
                isAmbient = isAmbient,
                onStart = { startExercise() },
                onStop  = { stopExercise() },
            )
        }
    }

    override fun onStart() {
        super.onStart()
        logLifecycle("onStart")
    }

    override fun onResume() {
        super.onResume()
        logLifecycle("onResume")
    }

    override fun onPause() {
        logLifecycle("onPause")
        super.onPause()
    }

    override fun onStop() {
        logLifecycle("onStop")
        super.onStop()
    }

    override fun onDestroy() {
        logLifecycle("onDestroy")
        super.onDestroy()
    }

    // AOD 진입
    override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) {
        isAmbient = true
        logLifecycle("onEnterAmbient")
    }

    // AOD 종료 (화면 활성화)
    override fun onExitAmbient() {
        isAmbient = false
        logLifecycle("onExitAmbient")
    }

    // AOD 주기적 갱신 (1분마다 호출)
    override fun onUpdateAmbient() {
        logLifecycle("onUpdateAmbient")
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

    private fun logLifecycle(event: String) {
        val interactive = powerManager?.isInteractive
        Log.d(
            "WatchApp",
            "[lifecycle] $event isAmbient=$isAmbient interactive=$interactive",
        )
    }
}

@Composable
fun WatchApp(isAmbient: Boolean, onStart: () -> Unit, onStop: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var isRunning by remember { mutableStateOf(false) }
    var pendingStart by remember { mutableStateOf(false) }

    fun startIfReady() {
        isRunning = true
        pendingStart = false
        onStart()
    }

    fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun hasForegroundPermissions(): Boolean {
        return WatchPermissionPolicy.foregroundPermissions(Build.VERSION.SDK_INT)
            .all(::hasPermission)
    }

    fun hasBackgroundSensorPermission(): Boolean {
        val backgroundPermission = WatchPermissionPolicy.backgroundSensorPermission(Build.VERSION.SDK_INT)
        return backgroundPermission == null || hasPermission(backgroundPermission)
    }

    fun openBackgroundSensorSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        Log.d("WatchApp", "[permission] opening app settings for background sensor")
        context.startActivity(intent)
        Toast.makeText(
            context,
            "앱 정보 > 권한 > 센서에서 '항상 허용'을 켜주세요.",
            Toast.LENGTH_LONG,
        ).show()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        Log.d("WatchApp", "[permission] foreground results=$grants")
        if (grants.values.all { it }) {
            val backgroundPermission = WatchPermissionPolicy.backgroundSensorPermission(Build.VERSION.SDK_INT)
            if (backgroundPermission != null && !hasPermission(backgroundPermission)) {
                openBackgroundSensorSettings()
            } else {
                startIfReady()
            }
        } else {
            pendingStart = false
            Toast.makeText(context, "권한이 필요합니다.", Toast.LENGTH_SHORT).show()
        }
    }

    DisposableEffect(lifecycleOwner, pendingStart) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && pendingStart) {
                Log.d(
                    "WatchApp",
                    "[permission] resumed pendingStart=$pendingStart foregroundReady=${hasForegroundPermissions()} backgroundReady=${hasBackgroundSensorPermission()}",
                )
                if (hasForegroundPermissions() && hasBackgroundSensorPermission()) {
                    startIfReady()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
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
                        pendingStart = false
                        onStop()
                    } else {
                        pendingStart = true
                        val required = WatchPermissionPolicy.foregroundPermissions(Build.VERSION.SDK_INT)
                        Log.d("WatchApp", "[permission] required foreground=$required")
                        if (hasForegroundPermissions()) {
                            if (hasBackgroundSensorPermission()) {
                                startIfReady()
                            } else {
                                val backgroundPermission = WatchPermissionPolicy.backgroundSensorPermission(Build.VERSION.SDK_INT)
                                if (backgroundPermission != null) {
                                    openBackgroundSensorSettings()
                                }
                            }
                        } else {
                            permissionLauncher.launch(required.toTypedArray())
                        }
                    }
                }) {
                    Text(text = if (isRunning) "중지" else "시작")
                }
            }
        }
    }
}
