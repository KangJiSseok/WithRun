package com.runninghealth.wear.exercise

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.HealthServices
import androidx.health.services.client.HealthServicesClient
import androidx.health.services.client.clearUpdateCallback
import androidx.health.services.client.endExercise
import androidx.health.services.client.getCapabilities
import androidx.health.services.client.pauseExercise
import androidx.health.services.client.resumeExercise
import androidx.health.services.client.startExercise
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.data.ExerciseUpdate
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.runninghealth.wear.R
import com.runninghealth.wear.data.CachedMetrics
import com.runninghealth.wear.data.DataLayerPaths
import com.runninghealth.wear.data.HealthPayload
import java.util.UUID
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow

class ExerciseForegroundService : Service() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val callbackExecutor = Executors.newSingleThreadExecutor()
    private var healthServicesClient: HealthServicesClient? = null
    private var lastSentAtMs: Long = 0L
    private var sessionId: String = UUID.randomUUID().toString()
    private var hasStartedExercise = false

    private val exerciseUpdateCallback =
        object : ExerciseUpdateCallback {
            override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
                Log.i(TAG, "=== onExerciseUpdateReceived ===")
                Log.d(TAG, "Exercise state: ${update.exerciseStateInfo.state.name}")

                val freshMetrics = HealthPayload.extractMetrics(update)
                val mergedMetrics = ExerciseRepository.mergeAndCacheMetrics(freshMetrics)

                val payload =
                    HealthPayload.fromExerciseUpdate(
                        update = update,
                        sessionId = sessionId,
                        deviceName = Build.MODEL ?: "Wear OS",
                        batteryPercent = readBatteryPercent(),
                        metrics = mergedMetrics,
                    )

                Log.d(TAG, "Payload: sessionId=${payload.sessionId}, state=${payload.exerciseState}")
                Log.d(
                    TAG,
                    "Metrics - HR: ${payload.heartRate?.value}bpm, Distance: ${payload.distance?.value}m, Calories: ${payload.calories?.value}kcal, Steps: ${payload.steps?.value}, Elevation: ${payload.elevationGain?.value}m, Location: ${payload.location?.latitude},${payload.location?.longitude}",
                )

                ExerciseRepository.exerciseState.value = payload.exerciseState
                ExerciseRepository.lastPayload.value = payload

                val nowMs = System.currentTimeMillis()
                if (nowMs - lastSentAtMs >= ExerciseRepository.sendIntervalMs) {
                    lastSentAtMs = nowMs
                    Log.d(TAG, "Throttle passed, sending payload to phone...")
                    serviceScope.launch {
                        sendHealthPayload(payload)
                    }
                } else {
                    Log.v(TAG, "Throttled: ${nowMs - lastSentAtMs}ms < ${ExerciseRepository.sendIntervalMs}ms")
                }
            }

            override fun onAvailabilityChanged(dataType: DataType<*, *>, availability: Availability) {
                val availabilitySummary = availability.toString()
                Log.d(
                    TAG,
                    "onAvailabilityChanged: ${dataType.name} -> ${availability.javaClass.simpleName}, details=$availabilitySummary",
                )
                ExerciseRepository.availabilityState.value =
                    "${dataType.name}:${availability.javaClass.simpleName}:$availabilitySummary"
            }

            override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) {
                Log.d(TAG, "onLapSummaryReceived: lapCount=${lapSummary.lapCount}")
                ExerciseRepository.availabilityState.value = "LAP_SUMMARY_RECEIVED"
            }

            override fun onRegistered() {
                Log.i(TAG, "=== onRegistered === Exercise callback registered successfully")
                ExerciseRepository.availabilityState.value = "CALLBACK_REGISTERED"
            }

            override fun onRegistrationFailed(throwable: Throwable) {
                Log.e(TAG, "=== onRegistrationFailed ===", throwable)
                ExerciseRepository.exerciseState.value = "UNKNOWN"
                ExerciseRepository.availabilityState.value = "REGISTRATION_FAILED"
            }
        }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        ExerciseRepository.serviceRunning.value = true
        observePauseResumeChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification =
            Notification.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setContentTitle("Running Health")
                .setContentText("Tracking active running session")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setOngoing(true)
                .build()
        val hasLocationPermissions = hasLocationPermissionsForBackgroundExercise()
        val foregroundServiceTypes =
            if (hasLocationPermissions) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
            }
        Log.i(
            TAG,
            "onStartCommand: startForeground types=$foregroundServiceTypes, hasLocationPermissions=$hasLocationPermissions",
        )
        startForeground(
            NOTIFICATION_ID,
            notification,
            foregroundServiceTypes,
        )

        when (intent?.action) {
            ACTION_STOP -> {
                serviceScope.launch {
                    stopExercise()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
                return START_NOT_STICKY
            }

            ACTION_PAUSE -> {
                serviceScope.launch { pauseExercise() }
                return START_STICKY
            }

            ACTION_RESUME -> {
                serviceScope.launch { resumeExercise() }
                return START_STICKY
            }

            else -> {
                serviceScope.launch {
                    initializeClientIfNeeded()
                    checkCapabilitiesAndStart()
                }
            }
        }

        return START_STICKY
    }

    override fun onDestroy() {
        ExerciseRepository.serviceRunning.value = false
        ExerciseRepository.clearCachedMetrics()
        serviceScope.launch {
            runCatching {
                healthServicesClient?.exerciseClient?.clearUpdateCallback(exerciseUpdateCallback)
            }
        }
        callbackExecutor.shutdown()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun observePauseResumeChannel() {
        serviceScope.launch {
            for (shouldPause in ExerciseRepository.pauseResumeChannel) {
                if (shouldPause) {
                    pauseExercise()
                } else {
                    resumeExercise()
                }
            }
        }
    }

    private fun createNotificationChannel() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel =
            NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Running Health",
                NotificationManager.IMPORTANCE_LOW,
            )
        notificationManager.createNotificationChannel(channel)
    }

    private fun initializeClientIfNeeded() {
        if (healthServicesClient == null) {
            healthServicesClient = HealthServices.getClient(this)
        }
    }

    private suspend fun checkCapabilitiesAndStart() {
        val client = healthServicesClient?.exerciseClient
        if (client == null) {
            Log.e(TAG, "checkCapabilitiesAndStart: exerciseClient is null!")
            return
        }
        Log.i(TAG, "checkCapabilitiesAndStart: Checking exercise capabilities...")
        ExerciseRepository.capabilityState.value = "CHECKING"

        val missingRequiredPermissions = missingRequiredExercisePermissions()
        if (missingRequiredPermissions.isNotEmpty()) {
            val missingPermissionSummary = missingRequiredPermissions.joinToString(",")
            Log.w(TAG, "Missing required exercise permissions: $missingPermissionSummary")
            ExerciseRepository.permissionState.value = "MISSING:$missingPermissionSummary"
            ExerciseRepository.capabilityState.value = "PERMISSION_DENIED"
            ExerciseRepository.exerciseState.value = "PERMISSION_DENIED"
            return
        }
        ExerciseRepository.permissionState.value = "GRANTED"
        val hasFineLocationPermission = hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        val hasBackgroundLocationPermission =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                true
            }
        Log.i(
            TAG,
            "Location permission detail: fine=$hasFineLocationPermission, background=$hasBackgroundLocationPermission",
        )

        runCatching {
            val capabilities = client.getCapabilities()
            Log.d(TAG, "Supported exercise types: ${capabilities.supportedExerciseTypes}")
            if (ExerciseType.RUNNING !in capabilities.supportedExerciseTypes) {
                Log.w(TAG, "RUNNING exercise type NOT supported!")
                ExerciseRepository.capabilityState.value = "NOT_SUPPORTED"
                return
            }

            val runningCapabilities = capabilities.getExerciseTypeCapabilities(ExerciseType.RUNNING)
            Log.d(TAG, "RUNNING supportedDataTypes: ${runningCapabilities.supportedDataTypes}")
            val supportedDataTypes = runningCapabilities.supportedDataTypes
            val supportsLocation = DataType.LOCATION in supportedDataTypes
            val supportsElevationGainTotal = DataType.ELEVATION_GAIN_TOTAL in supportedDataTypes
            Log.i(
                TAG,
                "RUNNING support flags - LOCATION=$supportsLocation, ELEVATION_GAIN_TOTAL=$supportsElevationGainTotal",
            )
            val missingRequiredDataTypes = REQUIRED_DATA_TYPES.filterNot { it in supportedDataTypes }
            if (missingRequiredDataTypes.isNotEmpty()) {
                Log.w(
                    TAG,
                    "Required data types missing. Required=${REQUIRED_DATA_TYPES.map { it.name }}, Missing=${missingRequiredDataTypes.map { it.name }}",
                )
                ExerciseRepository.capabilityState.value = "NOT_SUPPORTED"
                return
            }

            var supportedOptionalDataTypes = OPTIONAL_DATA_TYPES.filter { it in supportedDataTypes }.toSet()
            val hasLocationPermissions = hasLocationPermissionsForBackgroundExercise()
            if (supportsLocation && !hasLocationPermissions) {
                supportedOptionalDataTypes = supportedOptionalDataTypes - DataType.LOCATION
                Log.w(
                    TAG,
                    "Dropping LOCATION from exercise data types because background location permission is missing",
                )
            }

            val missingOptionalDataTypes = OPTIONAL_DATA_TYPES - supportedOptionalDataTypes
            if (missingOptionalDataTypes.isNotEmpty()) {
                Log.w(TAG, "Optional data types unavailable on this device: ${missingOptionalDataTypes.map { it.name }}")
            }

            val selectedDataTypes = REQUIRED_DATA_TYPES + supportedOptionalDataTypes
            val requestsLocation = DataType.LOCATION in selectedDataTypes
            val requestsElevationGainTotal = DataType.ELEVATION_GAIN_TOTAL in selectedDataTypes
            Log.i(
                TAG,
                "Selected data types for exercise: required=${REQUIRED_DATA_TYPES.map { it.name }}, optional=${supportedOptionalDataTypes.map { it.name }}",
            )
            Log.i(
                TAG,
                "Selected data type flags - LOCATION requested=$requestsLocation, ELEVATION_GAIN_TOTAL requested=$requestsElevationGainTotal, hasLocationPermissions=$hasLocationPermissions",
            )

            ExerciseRepository.capabilityState.value =
                if (missingOptionalDataTypes.isEmpty()) {
                    "SUPPORTED"
                } else {
                    "SUPPORTED_OPTIONAL_MISSING:${missingOptionalDataTypes.joinToString(",") { it.name }}"
                }
            startExercise(selectedDataTypes)
        }.onFailure { e ->
            Log.e(TAG, "checkCapabilitiesAndStart failed", e)
            ExerciseRepository.capabilityState.value = "NOT_SUPPORTED"
            ExerciseRepository.exerciseState.value = "UNKNOWN"
        }
    }

    suspend fun startExercise(dataTypes: Set<DataType<*, *>>) {
        if (hasStartedExercise) {
            Log.w(TAG, "startExercise: Exercise already started, skipping")
            return
        }

        val client = healthServicesClient?.exerciseClient
        if (client == null) {
            Log.e(TAG, "startExercise: exerciseClient is null!")
            return
        }
        Log.i(TAG, "startExercise: Starting RUNNING exercise...")

        Log.i(TAG, "startExercise: requesting dataTypes=${dataTypes.map { it.name }}")
        val isGpsEnabled = dataTypes.contains(DataType.LOCATION)
        Log.i(TAG, "startExercise: LOCATION requested=$isGpsEnabled, isGpsEnabled=$isGpsEnabled")
        val config =
            ExerciseConfig.builder(ExerciseType.RUNNING)
                .setDataTypes(dataTypes)
                .setIsGpsEnabled(isGpsEnabled)
                .setIsAutoPauseAndResumeEnabled(false)
                .build()

        runCatching {
            client.setUpdateCallback(callbackExecutor, exerciseUpdateCallback)
            client.startExercise(config)
            sessionId = UUID.randomUUID().toString()
            hasStartedExercise = true
            ExerciseRepository.clearCachedMetrics()
            Log.i(TAG, "startExercise: Exercise started successfully, sessionId=$sessionId")
            ExerciseRepository.exerciseState.value = "PREPARING"
        }.onFailure { e ->
            Log.e(TAG, "startExercise failed", e)
            ExerciseRepository.exerciseState.value = "UNKNOWN"
        }
    }

    suspend fun stopExercise() {
        val client = healthServicesClient?.exerciseClient ?: return
        runCatching {
            client.endExercise()
        }
        runCatching {
            client.clearUpdateCallback(exerciseUpdateCallback)
        }
        hasStartedExercise = false
        ExerciseRepository.clearCachedMetrics()
        ExerciseRepository.exerciseState.value = "ENDED"
    }

    suspend fun pauseExercise() {
        val client = healthServicesClient?.exerciseClient ?: return
        runCatching {
            client.pauseExercise()
            ExerciseRepository.exerciseState.value = "PAUSED"
        }
    }

    suspend fun resumeExercise() {
        val client = healthServicesClient?.exerciseClient ?: return
        runCatching {
            client.resumeExercise()
            ExerciseRepository.exerciseState.value = "ACTIVE"
        }
    }

    private fun missingRequiredExercisePermissions(): List<String> {
        val missingPermissions = mutableListOf<String>()

        if (!hasPermission(Manifest.permission.BODY_SENSORS)) {
            missingPermissions += Manifest.permission.BODY_SENSORS
        }
        if (!hasPermission(Manifest.permission.ACTIVITY_RECOGNITION)) {
            missingPermissions += Manifest.permission.ACTIVITY_RECOGNITION
        }
        if (requiresBackgroundBodySensorsPermission() && !hasPermission(Manifest.permission.BODY_SENSORS_BACKGROUND)) {
            missingPermissions += Manifest.permission.BODY_SENSORS_BACKGROUND
        }

        return missingPermissions
    }

    private fun hasLocationPermissionsForBackgroundExercise(): Boolean {
        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
            return false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !hasPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
            return false
        }
        return true
    }

    private fun requiresBackgroundBodySensorsPermission(): Boolean {
        return Build.VERSION.SDK_INT in 33..35
    }

    private fun hasPermission(permission: String): Boolean {
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }

    private suspend fun sendHealthPayload(payload: HealthPayload) {
        Log.d(TAG, "sendHealthPayload: Sending to path=${DataLayerPaths.HEALTH_UPDATE_PATH}")
        runCatching {
            val dataClient = Wearable.getDataClient(this)
            val connectedNodes = Wearable.getNodeClient(this).connectedNodes.await()
            if (connectedNodes.isEmpty()) {
                Log.w(TAG, "sendHealthPayload: no connected phone nodes; DataItem may sync later when connected")
            } else {
                val nodeSummary = connectedNodes.joinToString { "${it.displayName}(${it.id})" }
                Log.d(TAG, "sendHealthPayload: connected nodes=$nodeSummary")
            }
            val putDataMapRequest = PutDataMapRequest.create(DataLayerPaths.HEALTH_UPDATE_PATH)
            val json = payload.toJson()
            putDataMapRequest.dataMap.putString(DataLayerPaths.KEY_PAYLOAD, json)
            dataClient.putDataItem(putDataMapRequest.asPutDataRequest().setUrgent()).await()
            Log.i(TAG, "sendHealthPayload: SUCCESS - sent ${json.length} bytes")
        }.onFailure { e ->
            Log.e(TAG, "sendHealthPayload: FAILED", e)
        }
    }

    private fun readBatteryPercent(): Int {
        val batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
        return batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0, 100)
    }

    companion object {
        private const val TAG = "RH-ExerciseService"
        private const val NOTIFICATION_CHANNEL_ID = "running_health_channel"
        private const val NOTIFICATION_ID = 1001

        private val REQUIRED_DATA_TYPES: Set<DataType<*, *>> =
            setOf(
                DataType.HEART_RATE_BPM,
                DataType.DISTANCE_TOTAL,
                DataType.CALORIES_TOTAL,
            )

        private val OPTIONAL_DATA_TYPES: Set<DataType<*, *>> =
            setOf(
                DataType.DISTANCE,
                DataType.SPEED,
                DataType.PACE,
                DataType.STEPS_TOTAL,
                DataType.LOCATION,
                DataType.ELEVATION_GAIN_TOTAL,
            )

        private const val ACTION_START = "com.runninghealth.wear.exercise.START"
        private const val ACTION_STOP = "com.runninghealth.wear.exercise.STOP"
        private const val ACTION_PAUSE = "com.runninghealth.wear.exercise.PAUSE"
        private const val ACTION_RESUME = "com.runninghealth.wear.exercise.RESUME"

        fun startService(context: Context) {
            val intent = Intent(context, ExerciseForegroundService::class.java).setAction(ACTION_START)
            context.startForegroundService(intent)
        }

        fun stopService(context: Context) {
            val intent = Intent(context, ExerciseForegroundService::class.java).setAction(ACTION_STOP)
            context.startService(intent)
        }

        fun pauseService(context: Context) {
            val intent = Intent(context, ExerciseForegroundService::class.java).setAction(ACTION_PAUSE)
            context.startService(intent)
        }

        fun resumeService(context: Context) {
            val intent = Intent(context, ExerciseForegroundService::class.java).setAction(ACTION_RESUME)
            context.startService(intent)
        }
    }
}

object ExerciseRepository {
    val exerciseState = MutableStateFlow("UNKNOWN")
    val lastPayload = MutableStateFlow<HealthPayload?>(null)
    val capabilityState = MutableStateFlow("CHECKING")
    val permissionState = MutableStateFlow("UNKNOWN")
    val availabilityState = MutableStateFlow("UNKNOWN")
    val serviceRunning = MutableStateFlow(false)
    val pauseResumeChannel = Channel<Boolean>(capacity = Channel.BUFFERED)
    var sendIntervalMs: Long = 1000L

    @Volatile
    private var cachedMetrics: CachedMetrics? = null

    @Synchronized
    fun mergeAndCacheMetrics(freshMetrics: CachedMetrics): CachedMetrics {
        val mergedMetrics = freshMetrics.withFallback(cachedMetrics)
        cachedMetrics = mergedMetrics
        return mergedMetrics
    }

    @Synchronized
    fun clearCachedMetrics() {
        cachedMetrics = null
    }
}
