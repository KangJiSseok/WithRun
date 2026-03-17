import { useEffect } from 'react'
import { AppState, NativeEventEmitter, NativeModules, Platform } from 'react-native'

export type WatchHealthData = {
  kind: 'live' | 'snapshot' | 'catchup'
  sessionId: string
  seq: number
  capturedAt: number
  timestamp: number
  heartRate: number
  speedMps: number
  distanceM: number
  caloriesKcal: number
  altitudeM: number
  cadenceSpm: number
}

export function useWatchBridge(onMessage: (message: string) => void) {
  useEffect(() => {
    if (Platform.OS !== 'android') return

    const { WatchBridge } = NativeModules
    if (!WatchBridge) {
      console.warn('[WatchBridge] WatchBridge 모듈을 찾을 수 없습니다.')
      return
    }

    const emitter = new NativeEventEmitter(WatchBridge)
    const subscription = emitter.addListener('WatchMessage', onMessage)
    WatchBridge.resyncBridge?.()

    const appStateSubscription = AppState.addEventListener('change', (state) => {
      if (state === 'active') {
        WatchBridge.resyncBridge?.()
      }
    })

    return () => {
      subscription.remove()
      appStateSubscription.remove()
    }
  }, [onMessage])
}
