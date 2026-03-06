package com.runninghealth.wear.data

import org.json.JSONObject

sealed class WatchCommand {
    data object StartExercise : WatchCommand()

    data object StopExercise : WatchCommand()

    data object PauseExercise : WatchCommand()

    data object ResumeExercise : WatchCommand()

    data class Ping(val id: String) : WatchCommand()

    data class SetInterval(val intervalMs: Long) : WatchCommand()

    companion object {
        fun fromJson(json: String): WatchCommand? {
            return runCatching {
                val jsonObject = JSONObject(json)
                when (jsonObject.optString("type")) {
                    "START_EXERCISE" -> StartExercise
                    "STOP_EXERCISE" -> StopExercise
                    "PAUSE_EXERCISE" -> PauseExercise
                    "RESUME_EXERCISE" -> ResumeExercise
                    "PING" -> {
                        val id = jsonObject.optString("id")
                        if (id.isBlank()) {
                            null
                        } else {
                            Ping(id)
                        }
                    }

                    "SET_INTERVAL" -> {
                        val intervalMs = jsonObject.optLong("intervalMs", -1L)
                        if (intervalMs > 0L) {
                            SetInterval(intervalMs)
                        } else {
                            null
                        }
                    }

                    else -> null
                }
            }.getOrNull()
        }
    }
}

data class WatchAck(
    val type: String,
    val commandType: String? = null,
    val status: String? = null,
    val errorMessage: String? = null,
    val id: String? = null,
) {
    fun toJson(): String {
        val json = JSONObject()
        json.put("type", type)
        json.put("commandType", commandType ?: JSONObject.NULL)
        json.put("status", status ?: JSONObject.NULL)
        json.put("errorMessage", errorMessage ?: JSONObject.NULL)
        json.put("id", id ?: JSONObject.NULL)
        return json.toString()
    }
}
