import {
  createWebView,
  bridge,
  postMessageSchema,
} from '@webview-bridge/react-native'
import { Linking, NativeModules, Platform, Vibration } from 'react-native'
import { voicePlayer } from '@/audio/voicePlayer'
import type { GpsLocation, VoiceCueKey, WatchHealthData, WatchRunStartPayload } from './types'

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

// 웹 <-> 모바일 간 브릿지 통신 함수
export const appBridge = bridge({
  async whoRU() {
    return "Hello, I'm native"
  },
  async startSocialLogin(url: string) {
    // 모바일은 백엔드가 mobile 전용 OAuth 진입점에서 딥링크 복귀 쿠키를 심어주도록 경로만 바꿔 연다.
    const mobileAuthorizationUrl = url.replace(
      '/oauth2/authorization/',
      '/mobile/oauth2/authorization/',
    )

    await Linking.openURL(mobileAuthorizationUrl)
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
  async playVoiceCue(key: VoiceCueKey) {
    await voicePlayer.play(key)
  },
  async stopVoiceCue() {
    await voicePlayer.stop()
  },
  async setVoiceEnabled(enabled: boolean) {
    voicePlayer.setEnabled(enabled)
  },
  async startWatchRunSync(payload: WatchRunStartPayload) {
    if (Platform.OS !== 'android') return

    const watchBridge = NativeModules.WatchBridge as
      | { startRunSync?: (payloadJson: string) => void }
      | undefined
    if (!watchBridge?.startRunSync) return

    watchBridge.startRunSync(JSON.stringify(payload))
  },
  async stopWatchRunSync() {
    if (Platform.OS !== 'android') return

    const watchBridge = NativeModules.WatchBridge as
      | { stopRunSync?: () => void }
      | undefined
    if (!watchBridge?.stopRunSync) return

    watchBridge.stopRunSync()
  },
})

// 웹뷰 클라이언트
export const { WebView, postMessage } = createWebView({
  bridge: appBridge,
  postMessageSchema: appPostMessageSchema,
  debug: __DEV__,
})
