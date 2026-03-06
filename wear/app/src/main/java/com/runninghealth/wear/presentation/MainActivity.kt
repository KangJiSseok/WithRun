package com.runninghealth.wear.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModelProvider
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.runninghealth.wear.exercise.ExerciseForegroundService
import com.runninghealth.wear.presentation.theme.WearTheme

class MainActivity : ComponentActivity() {
    private val exerciseViewModel: ExerciseViewModel by lazy {
        ViewModelProvider(this, ExerciseViewModel.Factory)[ExerciseViewModel::class.java]
    }

    private val exercisePermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.values.all { granted -> granted }) {
                requestRequiredPermissions()
            }
            updatePermissionStateFromSystem()
        }

    private val bodySensorsBackgroundPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                requestRequiredPermissions()
            }
            updatePermissionStateFromSystem()
        }

    private val fineLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                requestRequiredPermissions()
            }
            updatePermissionStateFromSystem()
        }

    private val backgroundLocationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            updatePermissionStateFromSystem()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestRequiredPermissions()
        setContent {
            WearTheme {
                WatchHomeScreen(
                    viewModel = exerciseViewModel,
                    onRequestPermissions = ::requestRequiredPermissions,
                )
            }
        }
    }

    private fun requestRequiredPermissions() {
        when {
            !hasExerciseForegroundPermissions() -> {
                exercisePermissionLauncher.launch(EXERCISE_FOREGROUND_PERMISSIONS)
            }

            shouldRequestBackgroundBodySensorsPermission() -> {
                bodySensorsBackgroundPermissionLauncher.launch(Manifest.permission.BODY_SENSORS_BACKGROUND)
            }

            shouldRequestFineLocationPermission() -> {
                fineLocationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }

            shouldRequestBackgroundLocationPermission() -> {
                backgroundLocationPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }

            else -> {
                updatePermissionStateFromSystem()
            }
        }
    }

    private fun hasPermission(permission: String): Boolean {
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasExerciseForegroundPermissions(): Boolean {
        return EXERCISE_FOREGROUND_PERMISSIONS.all { permission ->
            hasPermission(permission)
        }
    }

    private fun shouldRequestBackgroundBodySensorsPermission(): Boolean {
        if (Build.VERSION.SDK_INT !in 33..35) {
            return false
        }
        return !hasPermission(Manifest.permission.BODY_SENSORS_BACKGROUND)
    }

    private fun shouldRequestFineLocationPermission(): Boolean {
        return !hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private fun shouldRequestBackgroundLocationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return false
        }
        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            return false
        }
        return !hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
    }

    private fun hasExerciseRequiredPermissions(): Boolean {
        if (!hasExerciseForegroundPermissions()) {
            return false
        }
        if (Build.VERSION.SDK_INT in 33..35 && !hasPermission(Manifest.permission.BODY_SENSORS_BACKGROUND)) {
            return false
        }
        return true
    }

    private fun updatePermissionStateFromSystem() {
        exerciseViewModel.updatePermissionState(granted = hasExerciseRequiredPermissions())
    }

    companion object {
        private val EXERCISE_FOREGROUND_PERMISSIONS =
            arrayOf(
                Manifest.permission.BODY_SENSORS,
                Manifest.permission.ACTIVITY_RECOGNITION,
            )
    }
}

@Composable
fun WatchHomeScreen(viewModel: ExerciseViewModel, onRequestPermissions: () -> Unit) {
    val exerciseState by viewModel.exerciseState.collectAsState()
    val lastPayload by viewModel.lastPayload.collectAsState()
    val capabilityState by viewModel.capabilityState.collectAsState()
    val permissionState by viewModel.permissionState.collectAsState()
    val context = LocalContext.current
    val canStartExercise = permissionState == "GRANTED"

    AppScaffold {
        val listState = rememberTransformingLazyColumnState()

        ScreenScaffold(scrollState = listState) { contentPadding ->
            TransformingLazyColumn(state = listState, contentPadding = contentPadding) {
                item {
                    ListHeader(modifier = Modifier.fillMaxWidth()) {
                        Text(text = "Running Health")
                    }
                }

                item {
                    Text(
                        text = "Exercise state: $exerciseState",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    Text(
                        text = "Heart rate: ${formatMetric(lastPayload?.heartRate?.value)} bpm",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    Text(
                        text = "Distance: ${formatDistanceMetric(lastPayload?.distance?.value)} m",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    Text(
                        text = "Calories: ${formatMetric(lastPayload?.calories?.value)} kcal",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    Text(
                        text = "Speed: ${formatMetric(lastPayload?.speed?.value)} m/s",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    Text(
                        text = "Pace: ${formatPaceMetric(lastPayload?.pace?.value)} sec/km",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    Text(
                        text = "Steps: ${formatStepsMetric(lastPayload?.steps?.value)} steps",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    Text(
                        text = "Elevation gain: ${formatElevationMetric(lastPayload?.elevationGain?.value)} m",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    Text(
                        text = "Capabilities: $capabilityState",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    Text(
                        text = "Permissions: $permissionState",
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                item {
                    Button(
                        onClick = onRequestPermissions,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Grant permissions")
                    }
                }

                item {
                    Button(
                        onClick = { ExerciseForegroundService.startService(context) },
                        enabled = canStartExercise,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Start")
                    }
                }

                if (exerciseState == "PAUSED") {
                    item {
                        Button(
                            onClick = { ExerciseForegroundService.resumeService(context) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Resume")
                        }
                    }
                } else {
                    item {
                        Button(
                            onClick = { ExerciseForegroundService.pauseService(context) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Pause")
                        }
                    }
                }

                item {
                    Button(
                        onClick = { ExerciseForegroundService.stopService(context) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Stop")
                    }
                }
            }
        }
    }
}

private fun formatMetric(value: Double?): String {
    return if (value == null || !value.isFinite()) {
        "--"
    } else {
        String.format("%.1f", value)
    }
}

private fun formatDistanceMetric(value: Double?): String {
    return if (value == null || !value.isFinite()) {
        "--"
    } else {
        String.format("%.1f", value.coerceAtLeast(0.0))
    }
}

private fun formatPaceMetric(value: Double?): String {
    return if (value == null || !value.isFinite() || value <= 0.0) {
        "--"
    } else {
        String.format("%.1f", value)
    }
}

private fun formatStepsMetric(value: Double?): String {
    return if (value == null || !value.isFinite()) {
        "--"
    } else {
        String.format("%.0f", value.coerceAtLeast(0.0))
    }
}

private fun formatElevationMetric(value: Double?): String {
    return if (value == null || !value.isFinite()) {
        "--"
    } else {
        String.format("%.1f", value.coerceAtLeast(0.0))
    }
}
