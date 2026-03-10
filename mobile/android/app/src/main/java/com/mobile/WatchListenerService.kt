package com.mobile

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class WatchListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == "/watch-to-phone") {
            val message = String(messageEvent.data)
            // WatchBridgeModule의 companion object를 통해 JS에 이벤트 emit
            WatchBridgeModule.emitToJs(message)
        }
    }
}
