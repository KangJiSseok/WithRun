package com.mobile

import android.util.Log
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import java.util.concurrent.ConcurrentLinkedQueue

class WatchBridgeModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext),
    MessageClient.OnMessageReceivedListener {

    private val messageClient by lazy { Wearable.getMessageClient(reactContext) }

    override fun getName(): String = "WatchBridge"

    // 리스너 카운트 관리 (RN이 내부적으로 요구)
    @ReactMethod
    fun addListener(eventName: String) {}

    @ReactMethod
    fun removeListeners(count: Int) {}

    // 포그라운드: MessageClient 직접 수신
    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d("WatchBridge", "[1] MessageClient 직접 수신: path=${messageEvent.path}")
        if (messageEvent.path == "/health-data") {
            val message = String(messageEvent.data)
            Log.d("WatchBridge", "[2] 포그라운드 메시지 수신: $message")
            emitToJs(message)
        }
    }

    override fun initialize() {
        super.initialize()
        messageClient.addListener(this)
        Log.d("WatchBridge", "[init] MessageClient 포그라운드 리스너 등록 완료")
        // 백그라운드 수신 중 큐에 쌓인 메시지 플러시
        flushPendingMessages()
    }

    override fun onCatalystInstanceDestroy() {
        messageClient.removeListener(this)
        super.onCatalystInstanceDestroy()
    }

    companion object {
        private var appContext: ReactApplicationContext? = null
        // 백그라운드 수신 메시지 임시 저장 (최대 120개 = 약 2분치)
        private val pendingMessages = ConcurrentLinkedQueue<String>()
        private const val MAX_PENDING = 120

        fun setContext(context: ReactApplicationContext) {
            appContext = context
            Log.d("WatchBridge", "[init] ReactApplicationContext 등록 완료")
        }

        // WatchListenerService(백그라운드) 또는 포그라운드 직접 수신 → JS emit
        fun emitToJs(message: String) {
            val jsModule = appContext
                ?.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            if (jsModule == null) {
                // JS 브릿지 미준비(백그라운드) → 큐에 저장
                if (pendingMessages.size < MAX_PENDING) {
                    pendingMessages.add(message)
                }
                Log.w("WatchBridge", "[3] JS 미준비, 큐에 저장 (${pendingMessages.size}/${MAX_PENDING})")
                return
            }
            jsModule.emit("WatchMessage", message)
            Log.d("WatchBridge", "[3] JS로 WatchMessage emit 완료")
        }

        private fun flushPendingMessages() {
            if (pendingMessages.isEmpty()) return
            val jsModule = appContext
                ?.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                ?: return
            var count = 0
            while (pendingMessages.isNotEmpty()) {
                val msg = pendingMessages.poll() ?: break
                jsModule.emit("WatchMessage", msg)
                count++
            }
            Log.d("WatchBridge", "[init] 백그라운드 큐 플러시 완료: ${count}개 전송")
        }
    }

    init {
        setContext(reactContext)
    }
}
