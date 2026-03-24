import { useCallback, useRef } from 'react'
import { StyleSheet } from 'react-native'
import { SafeAreaView } from 'react-native-safe-area-context'
import { WebView as BridgeWebView, postMessage, useWatchBridge, useGpsLocation, useCadence } from '@/bridge'
import type { GpsLocation, WatchHealthData } from '@/bridge'
import useWebUri from './hooks/useWebUri'

export default function WebViewScreen() {
  const { uri } = useWebUri()
  const cadenceSpmRef = useRef<number | null>(null)

  // 워치 메시지 수신 → JSON 파싱 후 타입드 데이터로 웹에 전송 (HR + 칼로리만)
  const handleWatchMessage = useCallback((message: string) => {
    console.log('[WatchBridge][5] postMessage → 웹으로 전송:', message)
    const parsed = JSON.parse(message) as WatchHealthData
    postMessage('watchHealthData', parsed)
  }, [])

  // 모바일 GPS 수신 → 가속도계 케이던스 합산 후 웹에 전송
  const handleGpsLocation = useCallback((location: GpsLocation) => {
    postMessage('gpsLocation', { ...location, cadenceSpm: cadenceSpmRef.current })
  }, [])

  // 가속도계 케이던스 → ref에 저장 (GPS 이벤트 때 같이 전송)
  const handleCadence = useCallback((spm: number) => {
    cadenceSpmRef.current = spm
  }, [])

  useWatchBridge(handleWatchMessage)
  useGpsLocation(handleGpsLocation)
  useCadence(handleCadence)

  return (
    <SafeAreaView style={styles.container}>
      <BridgeWebView style={styles.container} source={{ uri }} />
    </SafeAreaView>
  )
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
})
