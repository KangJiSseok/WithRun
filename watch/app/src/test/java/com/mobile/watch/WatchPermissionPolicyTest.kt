package com.mobile.watch

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchPermissionPolicyTest {

    @Test
    fun sdk34_uses_body_sensors_in_foreground_and_background_permission_separately() {
        assertEquals(
            listOf(
                android.Manifest.permission.BODY_SENSORS,
                android.Manifest.permission.ACTIVITY_RECOGNITION,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
            ),
            WatchPermissionPolicy.foregroundPermissions(deviceSdk = 34),
        )
        assertEquals(
            WatchPermissionPolicy.BODY_SENSORS_BACKGROUND_PERMISSION,
            WatchPermissionPolicy.backgroundSensorPermission(deviceSdk = 34),
        )
    }

    @Test
    fun sdk36_switches_to_health_permission_and_no_legacy_background_permission() {
        assertTrue(
            WatchPermissionPolicy.foregroundPermissions(deviceSdk = 36)
                .contains(WatchPermissionPolicy.READ_HEART_RATE_PERMISSION),
        )
        assertNull(WatchPermissionPolicy.backgroundSensorPermission(deviceSdk = 36))
    }
}
