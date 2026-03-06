package com.runninghealth.mobile.wearable

import android.util.Log
import com.google.android.gms.wearable.Channel
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.WearableListenerService
import org.json.JSONObject

class WearDataListenerService : WearableListenerService() {
    private val TAG = "RH-WearListener"

    override fun onDataChanged(dataEvents: DataEventBuffer): Unit {
        Log.d(TAG, "onDataChanged: received ${dataEvents.count} events")
        for (dataEvent: DataEvent in dataEvents) {
            val path: String? = dataEvent.dataItem.uri.path
            Log.d(TAG, "onDataChanged: event type=${dataEvent.type}, path=$path")
            if (dataEvent.type == DataEvent.TYPE_CHANGED && path == WearDataPaths.HEALTH_UPDATE_PATH) {
                Log.d(TAG, "onDataChanged: TYPE_CHANGED event for path=$path")
                val payload: String? = DataMapItem.fromDataItem(dataEvent.dataItem)
                    .dataMap
                    .getString(WearDataPaths.KEY_PAYLOAD)
                if (payload != null) {
                    Log.i(TAG, "onDataChanged: emitting health update, payload length=${payload.length}")
                    WearBridgeStore.emitHealthUpdate(payload)
                } else {
                    Log.w(TAG, "onDataChanged: payload is null for path=$path")
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent): Unit {
        Log.d(TAG, "onMessageReceived: path=${messageEvent.path}")
        if (messageEvent.path == WearDataPaths.ACK_PATH) {
            val payload: String = String(messageEvent.data, Charsets.UTF_8)
            Log.i(TAG, "onMessageReceived: ACK received, payload length=${payload.length}")
            WearBridgeStore.emitAckReceived(payload)
        }
    }

    override fun onChannelOpened(channel: Channel): Unit {
        super.onChannelOpened(channel)
    }

    override fun onPeerConnected(node: Node): Unit {
        Log.i(TAG, "onPeerConnected: node=${node.displayName} (${node.id})")
        val payload: String = createConnectionStatusJson(connected = true, node = node)
        WearBridgeStore.emitConnectionStatus(payload)
    }

    override fun onPeerDisconnected(node: Node): Unit {
        Log.i(TAG, "onPeerDisconnected: node=${node.displayName} (${node.id})")
        val payload: String = createConnectionStatusJson(connected = false, node = node)
        WearBridgeStore.emitConnectionStatus(payload)
    }

    private fun createConnectionStatusJson(connected: Boolean, node: Node): String {
        return JSONObject()
            .put("connected", connected)
            .put("nodeId", node.id)
            .put("nodeName", node.displayName)
            .toString()
    }
}
