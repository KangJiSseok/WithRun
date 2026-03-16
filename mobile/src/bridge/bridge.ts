import {
  createWebView,
  bridge,
  postMessageSchema,
} from '@webview-bridge/react-native'
import { GpsLocation, WatchHealthData } from './hooks'

// android <-> RN 간 브릿지 통신 함수
export const appPostMessageSchema = postMessageSchema({
  watchMessage: {
    validate: (data: unknown) => data as { message: string },
  },
  gpsLocation: {
    validate: (data: unknown) => data as GpsLocation,
  },
  watchHealthData: {
    validate: (data: unknown) => data as WatchHealthData,
  },
})

export type AndroidPostMessageSchema = typeof appPostMessageSchema

// 웹 <-> 모바일 간 브릿지 통신 함수
export const appBridge = bridge({
  async whoRU() {
    return "Hello, I'm native"
  },
})

// 브릿지 타입(웹 공유용)
export type AppBridge = typeof appBridge

// 웹뷰 클라이언트
export const { WebView, postMessage } = createWebView({
  bridge: appBridge,
  postMessageSchema: appPostMessageSchema,
  debug: __DEV__,
})
