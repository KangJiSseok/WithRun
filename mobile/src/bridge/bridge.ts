import { createWebView, bridge } from '@webview-bridge/react-native'

// 웹 <-> 모바일 간 브릿지 통신 함수
export const appBridge = bridge({
  async whoRU() {
    return "Hello, I'm native"
  },
  async aPlusB(a: number, b: number) {
    return a + b
  },
})

// 브릿지 타입(웹 공유용)
export type AppBridge = typeof appBridge

// 웹뷰 클라이언트
export const { WebView } = createWebView({
  bridge: appBridge,
  debug: true, // Enable console.log visibility in the native WebView
})
