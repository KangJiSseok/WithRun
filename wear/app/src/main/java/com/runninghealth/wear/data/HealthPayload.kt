package com.runninghealth.wear.data

import android.os.SystemClock
import android.util.Log
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseUpdate
import androidx.health.services.client.data.HeartRateAccuracy
import androidx.health.services.client.data.SampleDataPoint
import org.json.JSONObject

data class MetricValue(
    val value: Double,
    val unit: String,
    val accuracy: String,
    val timestampMs: Long,
)

data class SimpleMetric(
    val value: Double,
    val unit: String,
    val timestampMs: Long,
)

data class LocationMetric(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double?,
    val timestampMs: Long,
)

data class CachedMetrics(
    val heartRate: MetricValue?,
    val distance: SimpleMetric?,
    val calories: SimpleMetric?,
    val speed: SimpleMetric?,
    val pace: SimpleMetric?,
    val steps: SimpleMetric?,
    val elevationGain: SimpleMetric?,
    val location: LocationMetric?,
) {
    fun withFallback(cachedMetrics: CachedMetrics?): CachedMetrics {
        if (cachedMetrics == null) {
            return this
        }

        return CachedMetrics(
            heartRate = heartRate ?: cachedMetrics.heartRate,
            distance = distance ?: cachedMetrics.distance,
            calories = calories ?: cachedMetrics.calories,
            speed = speed ?: cachedMetrics.speed,
            pace = pace ?: cachedMetrics.pace,
            steps = steps ?: cachedMetrics.steps,
            elevationGain = elevationGain ?: cachedMetrics.elevationGain,
            location = location ?: cachedMetrics.location,
        )
    }
}

data class HealthPayload(
    val type: String = "HEALTH_UPDATE",
    val sessionId: String,
    val exerciseState: String,
    val timestampMs: Long,
    val heartRate: MetricValue?,
    val distance: SimpleMetric?,
    val calories: SimpleMetric?,
    val speed: SimpleMetric?,
    val pace: SimpleMetric?,
    val steps: SimpleMetric?,
    val elevationGain: SimpleMetric?,
    val location: LocationMetric?,
    val deviceName: String,
    val batteryPercent: Int,
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("type", type)
        json.put("sessionId", sessionId)
        json.put("exerciseState", exerciseState)
        json.put("timestampMs", timestampMs)
        json.put("heartRate", heartRate?.toJsonObject() ?: JSONObject.NULL)
        json.put("distance", distance?.toJsonObject() ?: JSONObject.NULL)
        json.put("calories", calories?.toJsonObject() ?: JSONObject.NULL)
        json.put("speed", speed?.toJsonObject() ?: JSONObject.NULL)
        json.put("pace", pace?.toJsonObject() ?: JSONObject.NULL)
        json.put("steps", steps?.toJsonObject() ?: JSONObject.NULL)
        json.put("elevationGain", elevationGain?.toJsonObject() ?: JSONObject.NULL)
        json.put("location", location?.toJsonObject() ?: JSONObject.NULL)
        json.put("deviceName", deviceName)
        json.put("batteryPercent", batteryPercent)
        return json.toString()
    }

    companion object {
        private const val TAG = "RH-HealthPayload"

        fun extractMetrics(update: ExerciseUpdate): CachedMetrics {
            val nowMs = System.currentTimeMillis()
            val bootEpochMs = nowMs - SystemClock.elapsedRealtime()
            val latestMetrics = update.latestMetrics

            val heartRatePoint = latestMetrics.getData(DataType.HEART_RATE_BPM).lastOrNull()
            val heartRate =
                heartRatePoint?.let {
                    MetricValue(
                        value = it.value,
                        unit = "bpm",
                        accuracy = mapHeartRateAccuracy(it),
                        timestampMs = bootEpochMs + it.timeDurationFromBoot.toMillis(),
                    )
                }

            val distanceTotal = latestMetrics.getData(DataType.DISTANCE_TOTAL)
            val distanceDelta = latestMetrics.getData(DataType.DISTANCE).lastOrNull()
            val distance =
                when {
                    distanceTotal != null -> {
                        SimpleMetric(
                            value = distanceTotal.total.toDouble(),
                            unit = "meters",
                            timestampMs = distanceTotal.end.toEpochMilli(),
                        )
                    }

                    distanceDelta != null -> {
                        SimpleMetric(
                            value = distanceDelta.value,
                            unit = "meters",
                            timestampMs = bootEpochMs + distanceDelta.endDurationFromBoot.toMillis(),
                        )
                    }

                    else -> null
                }

            val caloriesPoint = latestMetrics.getData(DataType.CALORIES_TOTAL)
            val calories =
                caloriesPoint?.let {
                    SimpleMetric(
                        value = it.total.toDouble(),
                        unit = "kcal",
                        timestampMs = it.end.toEpochMilli(),
                    )
                }

            val stepsTotal = latestMetrics.getData(DataType.STEPS_TOTAL)
            val steps =
                stepsTotal?.let {
                    SimpleMetric(
                        value = it.total.toDouble(),
                        unit = "count",
                        timestampMs = it.end.toEpochMilli(),
                    )
                }

            val elevationGainTotal = latestMetrics.getData(DataType.ELEVATION_GAIN_TOTAL)
            val elevationGain =
                elevationGainTotal?.let {
                    SimpleMetric(
                        value = it.total.toDouble(),
                        unit = "meters",
                        timestampMs = it.end.toEpochMilli(),
                    )
                }

            val speedPoint = latestMetrics.getData(DataType.SPEED).lastOrNull()
            val speed =
                speedPoint?.let {
                    SimpleMetric(
                        value = it.value,
                        unit = "m_per_s",
                        timestampMs = bootEpochMs + it.timeDurationFromBoot.toMillis(),
                    )
                }

            val pacePoint = latestMetrics.getData(DataType.PACE).lastOrNull()
            val paceFromSpeed =
                speedPoint
                    ?.takeIf { it.value > 0.0 }
                    ?.let {
                        SimpleMetric(
                            value = 1000.0 / it.value,
                            unit = "seconds_per_km",
                            timestampMs = bootEpochMs + it.timeDurationFromBoot.toMillis(),
                        )
                    }
            val pace =
                pacePoint?.let {
                    SimpleMetric(
                        value = it.value / 1000.0,
                        unit = "seconds_per_km",
                        timestampMs = bootEpochMs + it.timeDurationFromBoot.toMillis(),
                    )
                } ?: paceFromSpeed

            val locationPoint = latestMetrics.getData(DataType.LOCATION).lastOrNull()
            val location =
                locationPoint?.let {
                    LocationMetric(
                        latitude = it.value.latitude,
                        longitude = it.value.longitude,
                        altitude = it.value.altitude.takeIf { altitude -> altitude.isFinite() },
                        timestampMs = bootEpochMs + it.timeDurationFromBoot.toMillis(),
                    )
                }

            val distanceSource =
                when {
                    distanceTotal != null -> "DISTANCE_TOTAL"
                    distanceDelta != null -> "DISTANCE"
                    else -> "NONE"
                }

            val paceSource =
                when {
                    pacePoint != null -> "PACE"
                    paceFromSpeed != null -> "SPEED_FALLBACK"
                    else -> "NONE"
                }

            Log.d(
                TAG,
                "Raw metrics - HR=${heartRatePoint?.value}, DISTANCE_TOTAL=${distanceTotal?.total}, DISTANCE=${distanceDelta?.value}, CALORIES_TOTAL=${caloriesPoint?.total}, STEPS_TOTAL=${stepsTotal?.total}, ELEVATION_GAIN_TOTAL=${elevationGainTotal?.total}, SPEED=${speedPoint?.value}, PACE=${pacePoint?.value}, LOCATION=${locationPoint?.value}",
            )
            Log.d(
                TAG,
                "Mapped metrics - distance=${distance?.value}m(source=$distanceSource), speed=${speed?.value}m/s, pace=${pace?.value}s/km(source=$paceSource), calories=${calories?.value}kcal, steps=${steps?.value}, elevationGain=${elevationGain?.value}m, location=${location?.latitude},${location?.longitude}",
            )

            return CachedMetrics(
                heartRate = heartRate,
                distance = distance,
                calories = calories,
                speed = speed,
                pace = pace,
                steps = steps,
                elevationGain = elevationGain,
                location = location,
            )
        }

        fun fromExerciseUpdate(
            update: ExerciseUpdate,
            sessionId: String,
            deviceName: String,
            batteryPercent: Int,
            metrics: CachedMetrics? = null,
        ): HealthPayload {
            val nowMs = System.currentTimeMillis()
            val resolvedMetrics = metrics ?: extractMetrics(update)

            return HealthPayload(
                sessionId = sessionId,
                exerciseState = mapExerciseState(update.exerciseStateInfo.state.name),
                timestampMs = nowMs,
                heartRate = resolvedMetrics.heartRate,
                distance = resolvedMetrics.distance,
                calories = resolvedMetrics.calories,
                speed = resolvedMetrics.speed,
                pace = resolvedMetrics.pace,
                steps = resolvedMetrics.steps,
                elevationGain = resolvedMetrics.elevationGain,
                location = resolvedMetrics.location,
                deviceName = deviceName,
                batteryPercent = batteryPercent,
            )
        }

        private fun mapExerciseState(rawState: String): String {
            return when {
                rawState == "PREPARING" || rawState == "USER_STARTING" -> "PREPARING"
                rawState == "ACTIVE" -> "ACTIVE"
                rawState.contains("PAUSED") ||
                    rawState.contains("PAUSING") ||
                    rawState.contains("RESUMING") -> "PAUSED"

                rawState == "ENDED" ||
                    rawState.contains("END") ||
                    rawState.contains("TERMINAT") -> "ENDED"

                else -> "UNKNOWN"
            }
        }

        private fun mapHeartRateAccuracy(point: SampleDataPoint<Double>): String {
            val heartRateAccuracy = point.accuracy as? HeartRateAccuracy ?: return "UNKNOWN"
            return when (heartRateAccuracy.sensorStatus.name) {
                "ACCURACY_HIGH" -> "HIGH"
                "ACCURACY_MEDIUM" -> "MEDIUM"
                "ACCURACY_LOW" -> "LOW"
                "NO_CONTACT" -> "NO_CONTACT"
                else -> "UNKNOWN"
            }
        }
    }
}

private fun MetricValue.toJsonObject(): JSONObject {
    val json = JSONObject()
    json.put("value", value)
    json.put("unit", unit)
    json.put("accuracy", accuracy)
    json.put("timestampMs", timestampMs)
    return json
}

private fun SimpleMetric.toJsonObject(): JSONObject {
    val json = JSONObject()
    json.put("value", value)
    json.put("unit", unit)
    json.put("timestampMs", timestampMs)
    return json
}

private fun LocationMetric.toJsonObject(): JSONObject {
    val json = JSONObject()
    json.put("latitude", latitude)
    json.put("longitude", longitude)
    json.put("altitude", altitude ?: JSONObject.NULL)
    json.put("timestampMs", timestampMs)
    return json
}
