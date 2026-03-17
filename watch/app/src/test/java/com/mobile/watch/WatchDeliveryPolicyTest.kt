package com.mobile.watch

import androidx.health.services.client.data.BatchingMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchDeliveryPolicyTest {

    @Test
    fun sends_live_packets_during_aod_when_workout_is_active() {
        assertTrue(
            WatchDeliveryPolicy.shouldSendLivePacket(
                hasActiveWorkout = true,
                isInteractive = false,
            ),
        )
    }

    @Test
    fun does_not_send_live_packets_without_active_workout() {
        assertFalse(
            WatchDeliveryPolicy.shouldSendLivePacket(
                hasActiveWorkout = false,
                isInteractive = true,
            ),
        )
    }

    @Test
    fun uses_durable_sync_when_device_is_not_interactive() {
        assertTrue(WatchDeliveryPolicy.shouldUseDurableSync(isInteractive = false))
        assertFalse(WatchDeliveryPolicy.shouldUseDurableSync(isInteractive = true))
    }

    @Test
    fun enables_heart_rate_batching_when_supported() {
        assertEquals(
            setOf(BatchingMode.HEART_RATE_5_SECONDS),
            WatchDeliveryPolicy.selectBatchingModeOverrides(
                supportedModes = setOf(BatchingMode.HEART_RATE_5_SECONDS),
            ),
        )
    }

    @Test
    fun leaves_batching_empty_when_override_is_not_supported() {
        assertTrue(
            WatchDeliveryPolicy.selectBatchingModeOverrides(
                supportedModes = emptySet(),
            ).isEmpty(),
        )
    }
}
