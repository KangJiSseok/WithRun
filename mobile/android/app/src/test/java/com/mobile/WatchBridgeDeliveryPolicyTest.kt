package com.mobile

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchBridgeDeliveryPolicyTest {

    @Test
    fun queues_when_host_is_not_active_even_if_js_module_exists() {
        assertTrue(
            WatchBridgeDeliveryPolicy.shouldQueue(
                hasJsModule = true,
                isHostActive = false,
                listenerCount = 1,
            ),
        )
    }

    @Test
    fun queues_when_no_watch_message_listener_is_registered() {
        assertTrue(
            WatchBridgeDeliveryPolicy.shouldQueue(
                hasJsModule = true,
                isHostActive = true,
                listenerCount = 0,
            ),
        )
    }

    @Test
    fun dispatches_when_host_is_active_and_listener_is_registered() {
        assertFalse(
            WatchBridgeDeliveryPolicy.shouldQueue(
                hasJsModule = true,
                isHostActive = true,
                listenerCount = 1,
            ),
        )
    }
}
