package com.mobile

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService

class WatchListenerService : WearableListenerService() {

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d("WatchBridge", "[1] WatchListenerService.onMessageReceived: path=${messageEvent.path}")
        if (messageEvent.path == "/health-data") {
            // 포그라운드 시 WatchBridgeModule(MessageClient)이 직접 수신하므로 중복 emit 방지
            if (WatchBridgeModule.isListenerActive) {
                Log.d("WatchBridge", "[2] 포그라운드 활성 중 → WatchListenerService skip (중복 방지)")
                return
            }
            val message = String(messageEvent.data)
            Log.d("WatchBridge", "[2] 백그라운드 메시지 수신 완료: $message")
            // WatchBridgeModule의 companion object를 통해 JS로 이벤트 emit (큐잉)
            WatchBridgeModule.emitToJs(message)
        } else {
            Log.w("WatchBridge", "[1] 알 수 없는 path 무시됨: ${messageEvent.path}")
        }
    }
}
