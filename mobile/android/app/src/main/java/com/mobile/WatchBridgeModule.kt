package com.mobile

import android.util.Log
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.modules.core.DeviceEventManagerModule

class WatchBridgeModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String = "WatchBridge"

    // 리스너 카운트 관리 (RN이 내부적으로 요구)
    @ReactMethod
    fun addListener(eventName: String) {}

    @ReactMethod
    fun removeListeners(count: Int) {}

    companion object {
        private var appContext: ReactApplicationContext? = null

        fun setContext(context: ReactApplicationContext) {
            appContext = context
            Log.d("WatchBridge", "[init] ReactApplicationContext 등록 완료")
        }

        // WatchListenerService에서 호출 → JS로 이벤트 전달
        fun emitToJs(message: String) {
            Log.d("WatchBridge", "[3] emitToJs 호출: message=$message, appContext=${if (appContext != null) "있음" else "null"}")
            val jsModule = appContext
                ?.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            if (jsModule == null) {
                Log.e("WatchBridge", "[3] JS 모듈을 가져올 수 없음 (앱이 초기화되지 않음)")
                return
            }
            jsModule.emit("WatchMessage", message)
            Log.d("WatchBridge", "[3] JS로 WatchMessage 이벤트 emit 완료")
        }
    }

    init {
        setContext(reactContext)
    }
}
