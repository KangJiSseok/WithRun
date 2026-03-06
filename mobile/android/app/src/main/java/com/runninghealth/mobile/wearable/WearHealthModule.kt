package com.runninghealth.mobile.wearable

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.NodeClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

class WearHealthModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    private val moduleScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    init {
        WearBridgeStore.register(this)
    }

    override fun getName(): String = "WearHealth"

    override fun onCatalystInstanceDestroy(): Unit {
        super.onCatalystInstanceDestroy()
        WearBridgeStore.unregister()
        moduleScope.cancel()
    }

    override fun getConstants(): Map<String, Any> {
        return mapOf(
            "EVENT_HEALTH_UPDATE" to WearDataPaths.EVENT_HEALTH_UPDATE,
            "EVENT_ACK_RECEIVED" to WearDataPaths.EVENT_ACK_RECEIVED,
            "EVENT_CONNECTION_STATUS" to WearDataPaths.EVENT_CONNECTION_STATUS,
        )
    }

    fun emitEvent(eventName: String, data: String): Unit {
        val context: ReactApplicationContext = reactApplicationContext
        if (!context.hasActiveCatalystInstance()) {
            return
        }

        context
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, data)
    }

    @ReactMethod
    fun sendCommand(commandJson: String, promise: Promise): Unit {
        moduleScope.launch {
            try {
                val parsedCommand: JSONObject = JSONObject(commandJson)
                val commandType: String = parsedCommand.optString("type")
                if (commandType.isBlank()) {
                    throw IllegalArgumentException("Command type is required")
                }

                val nodeClient: NodeClient = Wearable.getNodeClient(reactApplicationContext)
                val connectedNodes: List<Node> = Tasks.await(nodeClient.connectedNodes)
                if (connectedNodes.isEmpty()) {
                    promise.reject("NO_CONNECTED_NODE", "No connected wearable node found")
                    return@launch
                }

                val messageClient: MessageClient = Wearable.getMessageClient(reactApplicationContext)
                for (node: Node in connectedNodes) {
                    Tasks.await(
                        messageClient.sendMessage(
                            node.id,
                            WearDataPaths.COMMAND_PATH,
                            commandJson.toByteArray(Charsets.UTF_8),
                        ),
                    )
                }

                promise.resolve("OK")
            } catch (exception: Exception) {
                promise.reject("SEND_COMMAND_FAILED", exception.message, exception)
            }
        }
    }

    @ReactMethod
    fun getConnectedNodes(promise: Promise): Unit {
        moduleScope.launch {
            try {
                val nodeClient: NodeClient = Wearable.getNodeClient(reactApplicationContext)
                val connectedNodes: List<Node> = Tasks.await(nodeClient.connectedNodes)
                val nodesJson: JSONArray = JSONArray()
                for (node: Node in connectedNodes) {
                    nodesJson.put(
                        JSONObject()
                            .put("id", node.id)
                            .put("displayName", node.displayName)
                            .put("isNearby", node.isNearby),
                    )
                }
                promise.resolve(nodesJson.toString())
            } catch (exception: Exception) {
                promise.reject("GET_CONNECTED_NODES_FAILED", exception.message, exception)
            }
        }
    }

    @ReactMethod
    fun checkConnectionStatus(promise: Promise): Unit {
        moduleScope.launch {
            try {
                val nodeClient: NodeClient = Wearable.getNodeClient(reactApplicationContext)
                val connectedNodes: List<Node> = Tasks.await(nodeClient.connectedNodes)
                val connectedNode: Node? = connectedNodes.firstOrNull()
                val statusJson: JSONObject = JSONObject()
                    .put("connected", connectedNode != null)
                    .put("nodeId", connectedNode?.id ?: "")
                    .put("nodeName", connectedNode?.displayName ?: "")
                promise.resolve(statusJson.toString())
            } catch (exception: Exception) {
                promise.reject("CHECK_CONNECTION_STATUS_FAILED", exception.message, exception)
            }
        }
    }

    @ReactMethod
    fun addListener(eventName: String): Unit {
        // Required by React Native event emitter contract.
    }

    @ReactMethod
    fun removeListeners(count: Double): Unit {
        // Required by React Native event emitter contract.
    }
}
