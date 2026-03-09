package com.mobile

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
        }

        // WatchListenerService에서 호출 → JS로 이벤트 전달
        fun emitToJs(message: String) {
            appContext
                ?.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                ?.emit("WatchMessage", message)
        }
    }

    init {
        setContext(reactContext)
    }
}
