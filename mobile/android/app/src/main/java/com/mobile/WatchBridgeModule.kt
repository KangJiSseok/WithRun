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
import org.json.JSONObject

class WatchBridgeModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext),
    MessageClient.OnMessageReceivedListener {

    private val messageClient by lazy { Wearable.getMessageClient(reactContext.applicationContext) }

    override fun getName(): String = "WatchBridge"

    // 리스너 카운트 관리 (RN이 내부적으로 요구)
    @ReactMethod
    fun addListener(eventName: String) {}

    @ReactMethod
    fun removeListeners(count: Int) {}

    @ReactMethod
    fun startRunSync(payloadJson: String) {
        sendToWatchNodes("/run/start", payloadJson)
    }

    @ReactMethod
    fun stopRunSync() {
        sendToWatchNodes("/run/stop", "{}")
    }

    @ReactMethod
    fun sendGpsToWatch(payloadJson: String) {
        val payload = try {
            JSONObject(payloadJson)
                .put("sentAt", System.currentTimeMillis())
                .toString()
        } catch (e: Exception) {
            payloadJson
        }
        sendToWatchNodes("/run/gps", payload)
    }

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
        isListenerActive = true
        messageClient.addListener(this)
            .addOnSuccessListener {
                Log.d("WatchBridge", "[init] MessageClient 리스너 등록 성공")
                // 등록 성공 후 연결된 워치 노드 확인
                Wearable.getNodeClient(reactContext.applicationContext).connectedNodes
                    .addOnSuccessListener { nodes ->
                        Log.d("WatchBridge", "[init] 연결된 워치 노드 수: ${nodes.size} → ${nodes.map { it.displayName }}")
                    }
                    .addOnFailureListener { e ->
                        Log.e("WatchBridge", "[init] 노드 확인 실패: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("WatchBridge", "[init] MessageClient 리스너 등록 실패: ${e.message}")
            }
        // 백그라운드 수신 중 큐에 쌓인 메시지 플러시
        flushPendingMessages()
    }

    override fun onCatalystInstanceDestroy() {
        isListenerActive = false
        messageClient.removeListener(this)
            .addOnSuccessListener { Log.d("WatchBridge", "[destroy] MessageClient 리스너 해제 성공") }
            .addOnFailureListener { e -> Log.e("WatchBridge", "[destroy] MessageClient 리스너 해제 실패: ${e.message}") }
        super.onCatalystInstanceDestroy()
    }

    companion object {
        private var appContext: ReactApplicationContext? = null
        // 백그라운드 수신 메시지 임시 저장 (최대 120개 = 약 2분치)
        private val pendingMessages = ConcurrentLinkedQueue<String>()
        private const val MAX_PENDING = 120

        // MessageClient 직접 리스너 활성화 여부
        // true(포그라운드): WatchBridgeModule이 직접 수신 → WatchListenerService는 skip
        // false(백그라운드): WatchListenerService만 수신 → pendingMessages에 큐잉
        @Volatile var isListenerActive = false

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

    private fun sendToWatchNodes(path: String, payload: String) {
        Wearable.getNodeClient(reactContext.applicationContext).connectedNodes
            .addOnSuccessListener { nodes ->
                if (nodes.isEmpty()) {
                    Log.w("WatchBridge", "[send] 연결된 워치 노드 없음: path=$path")
                    return@addOnSuccessListener
                }
                nodes.forEach { node ->
                    messageClient
                        .sendMessage(node.id, path, payload.toByteArray())
                        .addOnSuccessListener {
                            Log.d("WatchBridge", "[send] 전송 완료 path=$path → ${node.displayName}")
                        }
                        .addOnFailureListener { e ->
                            Log.e("WatchBridge", "[send] 전송 실패 path=$path → ${node.displayName}: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("WatchBridge", "[send] 노드 조회 실패 path=$path: ${e.message}")
            }
    }
}
