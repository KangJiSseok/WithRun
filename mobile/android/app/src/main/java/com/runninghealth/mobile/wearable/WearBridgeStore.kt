package com.runninghealth.mobile.wearable

import java.lang.ref.WeakReference

object WearBridgeStore {
    @Volatile
    private var moduleRef: WeakReference<WearHealthModule>? = null

    fun register(module: WearHealthModule): Unit {
        moduleRef = WeakReference(module)
    }

    fun unregister(): Unit {
        moduleRef = null
    }

    fun emitHealthUpdate(jsonPayload: String): Unit {
        moduleRef?.get()?.emitEvent(WearDataPaths.EVENT_HEALTH_UPDATE, jsonPayload)
    }

    fun emitAckReceived(jsonPayload: String): Unit {
        moduleRef?.get()?.emitEvent(WearDataPaths.EVENT_ACK_RECEIVED, jsonPayload)
    }

    fun emitConnectionStatus(jsonPayload: String): Unit {
        moduleRef?.get()?.emitEvent(WearDataPaths.EVENT_CONNECTION_STATUS, jsonPayload)
    }
}
