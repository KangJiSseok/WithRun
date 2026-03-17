package com.mobile

object WatchBridgeDeliveryPolicy {
    fun shouldQueue(hasJsModule: Boolean, isHostActive: Boolean, listenerCount: Int): Boolean {
        return !hasJsModule || !isHostActive || listenerCount <= 0
    }
}
