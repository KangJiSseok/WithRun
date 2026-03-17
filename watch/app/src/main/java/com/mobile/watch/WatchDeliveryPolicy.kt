package com.mobile.watch

import androidx.health.services.client.data.BatchingMode

internal object WatchDeliveryPolicy {
    fun shouldSendLivePacket(hasActiveWorkout: Boolean, isInteractive: Boolean): Boolean {
        return hasActiveWorkout
    }

    fun shouldUseDurableSync(isInteractive: Boolean): Boolean {
        return !isInteractive
    }

    fun selectBatchingModeOverrides(supportedModes: Set<BatchingMode>): Set<BatchingMode> {
        return if (supportedModes.contains(BatchingMode.HEART_RATE_5_SECONDS)) {
            setOf(BatchingMode.HEART_RATE_5_SECONDS)
        } else {
            emptySet()
        }
    }
}
