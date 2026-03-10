import { useEffect } from 'react'
import { NativeEventEmitter, NativeModules, Platform } from 'react-native'

export default function useWatchBridge(onMessage: (message: string) => void) {
  useEffect(() => {
    if (Platform.OS !== 'android') return

    const { WatchBridge } = NativeModules
    if (!WatchBridge) {
      console.warn('[useWatchBridge] WatchBridge 모듈을 찾을 수 없습니다.')
      return
    }

    const emitter = new NativeEventEmitter(WatchBridge)
    const subscription = emitter.addListener('WatchMessage', onMessage)

    return () => subscription.remove()
  }, [onMessage])
}
