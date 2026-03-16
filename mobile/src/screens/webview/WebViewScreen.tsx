import { useCallback } from 'react'
import { StyleSheet } from 'react-native'
import { WebView as BridgeWebView, postMessage, useWatchBridge, useGpsLocation } from '@/bridge'
import type { GpsLocation, WatchHealthData } from '@/bridge'
import useWebUri from './hooks/useWebUri'

export default function WebViewScreen() {
  const { uri } = useWebUri()

  // 워치 메시지 수신 → JSON 파싱 후 타입드 데이터로 웹에 전송
  const handleWatchMessage = useCallback((message: string) => {
    console.log('[WatchBridge][5] postMessage → 웹으로 전송:', message)
    const parsed = JSON.parse(message) as WatchHealthData
    postMessage('watchHealthData', parsed)
  }, [])

  // 모바일 GPS 수신 → 웹에 실시간 전송
  const handleGpsLocation = useCallback((location: GpsLocation) => {
    postMessage('gpsLocation', location)
  }, [])

  useWatchBridge(handleWatchMessage)
  useGpsLocation(handleGpsLocation)

  return <BridgeWebView style={styles.container} source={{ uri }} />
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
})
