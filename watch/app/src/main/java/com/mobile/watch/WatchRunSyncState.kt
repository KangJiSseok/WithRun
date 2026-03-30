package com.mobile.watch

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class WatchPoint(
    val latitude: Double,
    val longitude: Double,
)

data class WatchRunSyncData(
    val isActive: Boolean = false,
    val mode: String = "FREE",
    val runningSessionId: Int? = null,
    val courseCoordinates: List<WatchPoint> = emptyList(),
    val ghostCoordinates: List<WatchPoint> = emptyList(),
    val userTrail: List<WatchPoint> = emptyList(),
    val currentUserPoint: WatchPoint? = null,
    val routeProgressRatio: Float = 0f,
    val ghostProgressRatio: Float = 0f,
    val isOffCourse: Boolean = false,
    val headingDeg: Float? = null,
    val speedMps: Double = 0.0,
)

object WatchRunSyncState {
    private const val OFF_COURSE_THRESHOLD_M = 70.0

    private val _state = MutableStateFlow(WatchRunSyncData())
    val state: StateFlow<WatchRunSyncData> = _state.asStateFlow()

    fun startFromJson(payloadJson: String) {
        val payload = runCatching { JSONObject(payloadJson) }.getOrNull() ?: JSONObject()
        val mode = payload.optString("mode", "FREE")
        val runningSessionId = if (payload.has("runningSessionId")) payload.optInt("runningSessionId") else null
        val course = parsePointArray(payload.optJSONArray("courseCoordinates"))
        val ghost = parsePointArray(payload.optJSONArray("ghostCoordinates"))
        val seedUserPoint = course.firstOrNull()
        _state.value = WatchRunSyncData(
            isActive = true,
            mode = mode,
            runningSessionId = runningSessionId,
            courseCoordinates = course,
            ghostCoordinates = ghost,
            userTrail = seedUserPoint?.let { listOf(it) } ?: emptyList(),
            currentUserPoint = seedUserPoint,
            routeProgressRatio = 0f,
            ghostProgressRatio = 0f,
            isOffCourse = false,
        )
    }

    fun stop() {
        _state.value = _state.value.copy(isActive = false)
    }

    fun updateGpsFromJson(payloadJson: String) {
        val payload = runCatching { JSONObject(payloadJson) }.getOrNull() ?: return
        if (!_state.value.isActive) return

        val lat = payload.optDouble("latitude", Double.NaN)
        val lon = payload.optDouble("longitude", Double.NaN)
        if (lat.isNaN() || lon.isNaN()) return
        val heading = payload.optDouble("heading", Double.NaN)
        val speed = payload.optDouble("speedMps", 0.0)

        val next = WatchPoint(latitude = lat, longitude = lon)
        val prev = _state.value
        val (progressRatio, distanceToCourseM) = projectOnPolylineRatioAndDistance(next, prev.courseCoordinates)
        val ghostRatio = if (prev.ghostCoordinates.size >= 2) progressRatio else 0f
        _state.value = prev.copy(
            currentUserPoint = next,
            userTrail = prev.userTrail + next,
            routeProgressRatio = progressRatio,
            ghostProgressRatio = ghostRatio,
            isOffCourse = distanceToCourseM > OFF_COURSE_THRESHOLD_M,
            headingDeg = if (heading.isNaN()) null else heading.toFloat(),
            speedMps = speed,
        )
    }

    private fun parsePointArray(array: JSONArray?): List<WatchPoint> {
        if (array == null) return emptyList()
        val points = mutableListOf<WatchPoint>()
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val lat = item.optDouble("latitude", Double.NaN)
            val lon = item.optDouble("longitude", Double.NaN)
            if (lat.isNaN() || lon.isNaN()) continue
            points.add(WatchPoint(latitude = lat, longitude = lon))
        }
        return points
    }

    private fun projectOnPolylineRatioAndDistance(
        point: WatchPoint,
        polyline: List<WatchPoint>
    ): Pair<Float, Double> {
        if (polyline.size < 2) return 0f to 0.0

        val cumulative = DoubleArray(polyline.size)
        for (i in 1 until polyline.size) {
            cumulative[i] = cumulative[i - 1] + haversineM(polyline[i - 1], polyline[i])
        }
        val total = cumulative.last().takeIf { it > 1e-6 } ?: 1.0

        var minDist = Double.MAX_VALUE
        var projectedDistance = 0.0

        for (i in 0 until polyline.lastIndex) {
            val a = polyline[i]
            val b = polyline[i + 1]
            val (t, dist) = projectToSegment(point, a, b)
            if (dist < minDist) {
                minDist = dist
                val segLen = cumulative[i + 1] - cumulative[i]
                projectedDistance = cumulative[i] + segLen * t
            }
        }

        val ratio = (projectedDistance / total).coerceIn(0.0, 1.0).toFloat()
        return ratio to minDist
    }

    private fun projectToSegment(
        p: WatchPoint,
        a: WatchPoint,
        b: WatchPoint
    ): Pair<Double, Double> {
        val latScale = 111_320.0
        val lonScale = 111_320.0 * kotlin.math.cos(Math.toRadians(p.latitude))

        val ax = a.longitude * lonScale
        val ay = a.latitude * latScale
        val bx = b.longitude * lonScale
        val by = b.latitude * latScale
        val px = p.longitude * lonScale
        val py = p.latitude * latScale

        val abx = bx - ax
        val aby = by - ay
        val ab2 = abx * abx + aby * aby
        if (ab2 <= 1e-6) {
            val dist = haversineM(p, a)
            return 0.0 to dist
        }

        val rawT = ((px - ax) * abx + (py - ay) * aby) / ab2
        val t = rawT.coerceIn(0.0, 1.0)
        val cx = ax + abx * t
        val cy = ay + aby * t
        val dx = px - cx
        val dy = py - cy
        val distanceM = kotlin.math.sqrt(dx * dx + dy * dy)

        return t to distanceM
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
}
