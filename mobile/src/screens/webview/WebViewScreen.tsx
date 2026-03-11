import { useCallback } from 'react'
import { StyleSheet } from 'react-native'
import { WebView as BridgeWebView, postMessage } from '@/bridge/bridge'
import useWatchBridge from '@/hooks/useWatchBridge'
import useWebUri from './hooks/useWebUri'

export default function WebViewScreen() {
  const { uri } = useWebUri()

  // 워치 메시지 수신 → 웹에 CustomEvent 주입
  const handleWatchMessage = useCallback((message: string) => {
    console.log('[WatchBridge][5] postMessage → 웹으로 전송:', message)
    postMessage('watchMessage', { message })
  }, [])

  useWatchBridge(handleWatchMessage)

  return <BridgeWebView style={styles.container} source={{ uri }} />
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
})
