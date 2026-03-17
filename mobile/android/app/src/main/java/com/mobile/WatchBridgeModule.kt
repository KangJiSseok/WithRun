package com.mobile

import android.util.Log
import com.facebook.react.bridge.LifecycleEventListener
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.common.LifecycleState
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import java.util.concurrent.ConcurrentLinkedQueue
import org.json.JSONObject

class WatchBridgeModule(private val reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext),
    LifecycleEventListener,
    MessageClient.OnMessageReceivedListener,
    DataClient.OnDataChangedListener {

    private val messageClient by lazy { Wearable.getMessageClient(reactContext.applicationContext) }
    private val dataClient by lazy { Wearable.getDataClient(reactContext.applicationContext) }

    override fun getName(): String = "WatchBridge"

    // 리스너 카운트 관리 (RN이 내부적으로 요구)
    @ReactMethod
    fun addListener(eventName: String) {
        if (eventName != WATCH_MESSAGE_EVENT) {
            return
        }

        val listenerCount = incrementWatchMessageListenerCount()
        Log.d("WatchBridge", "[listener] WatchMessage 리스너 등록: $listenerCount")
        syncBridge("listener-added")
    }

    @ReactMethod
    fun removeListeners(count: Int) {
        val listenerCount = decrementWatchMessageListenerCount(count)
        Log.d("WatchBridge", "[listener] WatchMessage 리스너 해제 후 남은 수: $listenerCount")
    }

    @ReactMethod
    fun resyncBridge() {
        syncBridge("js-resync")
    }

    // 포그라운드: MessageClient 직접 수신
    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d("WatchBridge", "[1] MessageClient 직접 수신: path=${messageEvent.path}")
        when (messageEvent.path) {
            WatchSyncPaths.livePath, WatchSyncPaths.catchupPath -> {
                handlePayload(String(messageEvent.data), messageEvent.path)
            }
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type != DataEvent.TYPE_CHANGED) {
                continue
            }

            val path = event.dataItem.uri.path ?: continue
            if (path != WatchSyncPaths.snapshotPath) {
                continue
            }

            val payload = DataMapItem.fromDataItem(event.dataItem)
                .dataMap
                .getString(WatchSyncPaths.snapshotPayloadKey)
                ?: continue
            handlePayload(payload, path)
        }
    }

    override fun initialize() {
        super.initialize()
        reactContext.addLifecycleEventListener(this)
        setHostActive(reactContext.lifecycleState == LifecycleState.RESUMED)
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
        dataClient.addListener(this)
            .addOnSuccessListener { Log.d("WatchBridge", "[init] DataClient 리스너 등록 성공") }
            .addOnFailureListener { e -> Log.e("WatchBridge", "[init] DataClient 리스너 등록 실패: ${e.message}") }
        syncBridge("initialize")
    }

    @Deprecated("React Native lifecycle cleanup override")
    @Suppress("DEPRECATION")
    override fun onCatalystInstanceDestroy() {
        reactContext.removeLifecycleEventListener(this)
        messageClient.removeListener(this)
            .addOnSuccessListener { Log.d("WatchBridge", "[destroy] MessageClient 리스너 해제 성공") }
            .addOnFailureListener { e -> Log.e("WatchBridge", "[destroy] MessageClient 리스너 해제 실패: ${e.message}") }
        dataClient.removeListener(this)
            .addOnSuccessListener { Log.d("WatchBridge", "[destroy] DataClient 리스너 해제 성공") }
            .addOnFailureListener { e -> Log.e("WatchBridge", "[destroy] DataClient 리스너 해제 실패: ${e.message}") }
        clearContext(reactContext)
        setHostActive(false)
        resetWatchMessageListenerCount()
        super.onCatalystInstanceDestroy()
    }

    override fun onHostResume() {
        setHostActive(true)
        Log.d("WatchBridge", "[lifecycle] host resumed")
        syncBridge("host-resume")
    }

    override fun onHostPause() {
        setHostActive(false)
        Log.d("WatchBridge", "[lifecycle] host paused")
    }

    override fun onHostDestroy() {
        setHostActive(false)
        Log.d("WatchBridge", "[lifecycle] host destroyed")
    }

    private fun handlePayload(rawPayload: String, sourcePath: String) {
        Log.d("WatchBridge", "[2] 포그라운드 payload 수신: path=$sourcePath payload=$rawPayload")
        val normalized = WatchSyncState.accept(rawPayload)
        if (normalized == null) {
            Log.d("WatchBridge", "[2] 중복 또는 파싱 실패로 무시됨")
            return
        }
        emitToJs(normalized)
    }

    private fun requestCatchup() {
        val requestPayload = JSONObject()
            .put("lastSeenSeq", WatchSyncState.currentLastSeenSeq())
            .toString()
            .toByteArray()

        Wearable.getNodeClient(reactContext.applicationContext).connectedNodes
            .addOnSuccessListener { nodes ->
                nodes.forEach { node ->
                    messageClient.sendMessage(node.id, WatchSyncPaths.catchupRequestPath, requestPayload)
                        .addOnSuccessListener {
                            Log.d("WatchBridge", "[init] catch-up 요청 성공 → ${node.displayName}")
                        }
                        .addOnFailureListener { e ->
                            Log.e("WatchBridge", "[init] catch-up 요청 실패: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("WatchBridge", "[init] catch-up 노드 확인 실패: ${e.message}")
            }
    }

    private fun syncBridge(reason: String) {
        flushPendingMessages()
        emitLatestPayload()
        requestCatchup()
        Log.d("WatchBridge", "[sync] 브릿지 재동기화 실행: $reason")
    }

    companion object {
        private var appContext: ReactApplicationContext? = null
        // 백그라운드 수신 메시지 임시 저장 (최대 120개 = 약 2분치)
        private val pendingMessages = ConcurrentLinkedQueue<String>()
        private const val MAX_PENDING = 120
        private const val WATCH_MESSAGE_EVENT = "WatchMessage"

        @Volatile
        private var hostActive = false

        @Volatile
        private var watchMessageListenerCount = 0

        fun setContext(context: ReactApplicationContext) {
            appContext = context
            Log.d("WatchBridge", "[init] ReactApplicationContext 등록 완료")
        }

        fun clearContext(context: ReactApplicationContext) {
            if (appContext == context) {
                appContext = null
                Log.d("WatchBridge", "[destroy] ReactApplicationContext 해제 완료")
            }
        }

        fun setHostActive(active: Boolean) {
            hostActive = active
        }

        fun incrementWatchMessageListenerCount(): Int {
            watchMessageListenerCount += 1
            return watchMessageListenerCount
        }

        fun decrementWatchMessageListenerCount(count: Int): Int {
            watchMessageListenerCount = maxOf(0, watchMessageListenerCount - count)
            return watchMessageListenerCount
        }

        fun resetWatchMessageListenerCount() {
            watchMessageListenerCount = 0
        }

        // WatchListenerService(백그라운드) 또는 포그라운드 직접 수신 → JS emit
        fun emitToJs(message: String) {
            val context = appContext
            val hasActiveReactInstance = context?.hasActiveReactInstance() == true
            val jsModule = if (hasActiveReactInstance) {
                context?.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            } else {
                null
            }
            if (WatchBridgeDeliveryPolicy.shouldQueue(
                    hasJsModule = jsModule != null,
                    isHostActive = hostActive,
                    listenerCount = watchMessageListenerCount,
                )) {
                // JS 브릿지 미준비(백그라운드) → 큐에 저장
                if (pendingMessages.size < MAX_PENDING) {
                    pendingMessages.add(message)
                }
                Log.w(
                    "WatchBridge",
                    "[3] 브릿지 미준비, 큐에 저장 (${pendingMessages.size}/${MAX_PENDING}) hostActive=$hostActive listeners=$watchMessageListenerCount contextReady=${context != null}",
                )
                return
            }
            jsModule?.emit(WATCH_MESSAGE_EVENT, message)
            Log.d("WatchBridge", "[3] JS로 WatchMessage emit 완료")
        }

        private fun emitLatestPayload() {
            val latestPayload = WatchSyncState.latestPayloadJson() ?: return
            emitToJs(latestPayload)
        }

        private fun flushPendingMessages() {
            if (pendingMessages.isEmpty()) return
            val context = appContext
            val hasActiveReactInstance = context?.hasActiveReactInstance() == true
            val jsModule = if (hasActiveReactInstance) {
                context?.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            } else {
                null
            }
            if (WatchBridgeDeliveryPolicy.shouldQueue(
                    hasJsModule = jsModule != null,
                    isHostActive = hostActive,
                    listenerCount = watchMessageListenerCount,
                )) {
                Log.d(
                    "WatchBridge",
                    "[init] 플러시 보류: hostActive=$hostActive listeners=$watchMessageListenerCount pending=${pendingMessages.size}",
                )
                return
            }

            var count = 0
            while (pendingMessages.isNotEmpty()) {
                val msg = pendingMessages.poll() ?: break
                jsModule?.emit(WATCH_MESSAGE_EVENT, msg)
                count++
            }
            Log.d("WatchBridge", "[init] 백그라운드 큐 플러시 완료: ${count}개 전송")
        }
    }

    init {
        setContext(reactContext)
    }
}
