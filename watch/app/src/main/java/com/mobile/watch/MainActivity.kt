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
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.*
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

class MainActivity : ComponentActivity() {

    private val healthClient by lazy { HealthServices.getClient(this) }
    private val exerciseClient by lazy { healthClient.exerciseClient }

    // 수집할 운동 데이터 타입 목록
    private val exerciseConfig = ExerciseConfig(
        exerciseType = ExerciseType.RUNNING,
        dataTypes = setOf(
            DataType.HEART_RATE_BPM,       // 심박수 (bpm)
            DataType.SPEED,                 // 속도 (m/s)
            DataType.DISTANCE_TOTAL,        // 누적 거리 (m)
            DataType.CALORIES_TOTAL,        // 누적 칼로리 (kcal)
            DataType.STEPS_PER_MINUTE,      // 케이던스 (spm)
            DataType.ABSOLUTE_ELEVATION,    // 해발 고도 (m)
        ),
        isAutoPauseAndResumeEnabled = false,
        isGpsEnabled = false,
    )

    private val exerciseCallback = object : ExerciseUpdateCallback {

        override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
            val metrics = update.latestMetrics

            // 심박수가 없으면 해당 틱은 전송 스킵
            val heartRate = metrics.getData(DataType.HEART_RATE_BPM).lastOrNull()?.value ?: return

            val speedMps     = metrics.getData(DataType.SPEED).lastOrNull()?.value ?: 0.0
            val cadenceSpm   = metrics.getData(DataType.STEPS_PER_MINUTE).lastOrNull()?.value?.toInt() ?: 0
            val altitudeM    = metrics.getData(DataType.ABSOLUTE_ELEVATION).lastOrNull()?.value ?: 0.0
            val distanceM    = metrics.getData(DataType.DISTANCE_TOTAL)?.total ?: 0.0
            val caloriesKcal = metrics.getData(DataType.CALORIES_TOTAL)?.total ?: 0.0

            val json = JSONObject().apply {
                put("timestamp",    System.currentTimeMillis())
                put("heartRate",    heartRate.toInt())
                put("speedMps",     speedMps)
                put("distanceM",    distanceM)
                put("caloriesKcal", caloriesKcal)
                put("altitudeM",    altitudeM)
                put("cadenceSpm",   cadenceSpm)
            }

            sendToPhone(json.toString())
        }

        override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) {}

        override fun onAvailabilityChanged(dataType: DataType<*, *>, availability: Availability) {
            Log.d("WatchApp", "[$dataType] 가용성 변경: $availability")
        }

        override fun onRegistered() {
            Log.d("WatchApp", "ExerciseCallback 등록 완료")
        }

        override fun onRegistrationFailed(throwable: Throwable) {
            Log.e("WatchApp", "ExerciseCallback 등록 실패", throwable)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exerciseClient.setUpdateCallback(exerciseCallback)
        setContent {
            WatchApp(
                onStart = { startExercise() },
                onStop  = { stopExercise() },
            )
        }
    }

    private fun startExercise() {
        lifecycleScope.launch {
            try {
                exerciseClient.startExerciseAsync(exerciseConfig).await()
                showToast("운동 시작")
            } catch (e: Exception) {
                showToast("시작 실패: ${e.message}")
                Log.e("WatchApp", "운동 시작 실패", e)
            }
        }
    }

    private fun stopExercise() {
        lifecycleScope.launch {
            try {
                exerciseClient.endExerciseAsync().await()
                showToast("운동 종료")
            } catch (e: Exception) {
                Log.e("WatchApp", "운동 종료 실패", e)
            }
        }
    }

    private fun sendToPhone(json: String) {
        lifecycleScope.launch {
            try {
                val nodes = Wearable.getNodeClient(this@MainActivity).connectedNodes.await()
                nodes.forEach { node ->
                    Wearable.getMessageClient(this@MainActivity)
                        .sendMessage(node.id, "/health-data", json.toByteArray())
                        .await()
                }
            } catch (e: Exception) {
                Log.e("WatchApp", "전송 실패", e)
            }
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun WatchApp(onStart: () -> Unit, onStop: () -> Unit) {
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
