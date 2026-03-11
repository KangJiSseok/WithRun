import { useEffect } from 'react'
import { NativeEventEmitter, NativeModules, Platform } from 'react-native'

export default function useWatchBridge(onMessage: (message: string) => void) {
  useEffect(() => {
    if (Platform.OS !== 'android') return

    const { WatchBridge } = NativeModules
    if (!WatchBridge) {
      console.warn('[WatchBridge][4] WatchBridge 모듈을 찾을 수 없습니다.')
      return
    }

    console.log('[WatchBridge][4] WatchMessage 리스너 등록')
    const emitter = new NativeEventEmitter(WatchBridge)
    const subscription = emitter.addListener('WatchMessage', (message) => {
      console.log('[WatchBridge][4] WatchMessage 이벤트 수신:', message)
      onMessage(message)
    })

    return () => subscription.remove()
  }, [onMessage])
}
