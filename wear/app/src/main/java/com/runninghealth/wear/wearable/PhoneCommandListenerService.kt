package com.runninghealth.wear.wearable

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.runninghealth.wear.data.DataLayerPaths
import com.runninghealth.wear.data.WatchAck
import com.runninghealth.wear.data.WatchCommand
import com.runninghealth.wear.exercise.ExerciseForegroundService
import com.runninghealth.wear.exercise.ExerciseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PhoneCommandListenerService : WearableListenerService() {
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != DataLayerPaths.COMMAND_PATH) {
            return
        }

        val commandJson = String(messageEvent.data, Charsets.UTF_8)
        val command = WatchCommand.fromJson(commandJson)
        val sourceNodeId = messageEvent.sourceNodeId

        serviceScope.launch {
            handleCommand(command = command, sourceNodeId = sourceNodeId)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private suspend fun handleCommand(command: WatchCommand?, sourceNodeId: String) {
        when (command) {
            WatchCommand.StartExercise -> {
                if (!ExerciseRepository.serviceRunning.value) {
                    ExerciseForegroundService.startService(this)
                }
                sendAck(
                    sourceNodeId,
                    WatchAck(type = "ACK", commandType = "START_EXERCISE", status = "OK"),
                )
            }

            WatchCommand.StopExercise -> {
                ExerciseForegroundService.stopService(this)
                sendAck(
                    sourceNodeId,
                    WatchAck(type = "ACK", commandType = "STOP_EXERCISE", status = "OK"),
                )
            }

            WatchCommand.PauseExercise -> {
                if (ExerciseRepository.serviceRunning.value) {
                    ExerciseRepository.pauseResumeChannel.trySend(true)
                    sendAck(
                        sourceNodeId,
                        WatchAck(type = "ACK", commandType = "PAUSE_EXERCISE", status = "OK"),
                    )
                } else {
                    sendAck(
                        sourceNodeId,
                        WatchAck(
                            type = "ACK",
                            commandType = "PAUSE_EXERCISE",
                            status = "ERROR",
                            errorMessage = "Exercise service not running",
                        ),
                    )
                }
            }

            WatchCommand.ResumeExercise -> {
                if (ExerciseRepository.serviceRunning.value) {
                    ExerciseRepository.pauseResumeChannel.trySend(false)
                    sendAck(
                        sourceNodeId,
                        WatchAck(type = "ACK", commandType = "RESUME_EXERCISE", status = "OK"),
                    )
                } else {
                    sendAck(
                        sourceNodeId,
                        WatchAck(
                            type = "ACK",
                            commandType = "RESUME_EXERCISE",
                            status = "ERROR",
                            errorMessage = "Exercise service not running",
                        ),
                    )
                }
            }

            is WatchCommand.Ping -> {
                sendPong(sourceNodeId = sourceNodeId, id = command.id)
            }

            is WatchCommand.SetInterval -> {
                ExerciseRepository.sendIntervalMs = command.intervalMs
                sendAck(
                    sourceNodeId,
                    WatchAck(type = "ACK", commandType = "SET_INTERVAL", status = "OK"),
                )
            }

            null -> {
                sendAck(
                    sourceNodeId,
                    WatchAck(
                        type = "ACK",
                        commandType = "UNKNOWN",
                        status = "ERROR",
                        errorMessage = "Invalid command payload",
                    ),
                )
            }
        }
    }

    private suspend fun sendPong(sourceNodeId: String, id: String) {
        val pongPayload = WatchAck(type = "PONG", id = id)
        sendRawMessage(sourceNodeId = sourceNodeId, payload = pongPayload.toJson())
    }

    private suspend fun sendAck(sourceNodeId: String, ack: WatchAck) {
        sendRawMessage(sourceNodeId = sourceNodeId, payload = ack.toJson())
    }

    private suspend fun sendRawMessage(sourceNodeId: String, payload: String) {
        runCatching {
            Wearable.getMessageClient(this)
                .sendMessage(
                    sourceNodeId,
                    DataLayerPaths.ACK_PATH,
                    payload.toByteArray(Charsets.UTF_8),
                )
                .await()
        }
    }
}
