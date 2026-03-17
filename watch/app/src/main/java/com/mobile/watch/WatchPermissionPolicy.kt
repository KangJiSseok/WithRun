package com.mobile.watch

internal object WatchPermissionPolicy {
    const val READ_HEART_RATE_PERMISSION = "android.permission.health.READ_HEART_RATE"
    const val BODY_SENSORS_BACKGROUND_PERMISSION = "android.permission.BODY_SENSORS_BACKGROUND"

    fun foregroundPermissions(deviceSdk: Int): List<String> {
        val permissions = mutableListOf<String>()
        if (deviceSdk >= 36) {
            permissions += READ_HEART_RATE_PERMISSION
        } else {
            permissions += android.Manifest.permission.BODY_SENSORS
        }
        permissions += android.Manifest.permission.ACTIVITY_RECOGNITION
        permissions += android.Manifest.permission.ACCESS_FINE_LOCATION
        return permissions
    }

    fun backgroundSensorPermission(deviceSdk: Int): String? {
        return if (deviceSdk in 33..35) {
            BODY_SENSORS_BACKGROUND_PERMISSION
        } else {
            null
        }
    }
}
