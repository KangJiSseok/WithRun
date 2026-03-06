package com.runninghealth.mobile.wearable

object WearDataPaths {
    const val HEALTH_UPDATE_PATH: String = "/running-health/update"
    const val COMMAND_PATH: String = "/running-health/command"
    const val ACK_PATH: String = "/running-health/ack"
    const val KEY_PAYLOAD: String = "payload"
    const val EVENT_HEALTH_UPDATE: String = "WearHealthUpdate"
    const val EVENT_ACK_RECEIVED: String = "WearAckReceived"
    const val EVENT_CONNECTION_STATUS: String = "WearConnectionStatus"
}
