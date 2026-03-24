import {
  createWebView,
  bridge,
  postMessageSchema,
} from '@webview-bridge/react-native'
import { Vibration } from 'react-native'
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
  async notifyCourseDeviation(count: 1 | 2 | 3) {
    // count만큼 진동 반복: [대기, ON, 대기, ON, ...]
    // 패턴: 300ms 진동 + 100ms 대기 × count
    const pattern: number[] = [0]
    for (let i = 0; i < count; i++) {
      pattern.push(300) // 진동
      if (i < count - 1) pattern.push(100) // 연속 사이 간격
    }
    Vibration.vibrate(pattern)
  },
  async notifyCourseDeviationForceStop() {
    // 강제 종료 알림: 1000ms 단일 진동
    Vibration.vibrate(1000)
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
