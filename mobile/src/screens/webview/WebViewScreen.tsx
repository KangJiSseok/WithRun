import { useCallback, useRef } from 'react'
import { StyleSheet } from 'react-native'
import WebView from 'react-native-webview'
import { WebView as BridgeWebView } from '@/bridge/bridge'
import useWebUri from './hooks/useWebUri'
import useWatchBridge from '@/hooks/useWatchBridge'

export default function WebViewScreen() {
  const { uri } = useWebUri()
  const webViewRef = useRef<WebView>(null)

  // 워치 메시지 수신 → 웹에 CustomEvent 주입
  const handleWatchMessage = useCallback((message: string) => {
    webViewRef.current?.injectJavaScript(
      `window.dispatchEvent(new CustomEvent('watchMessage', { detail: ${JSON.stringify(message)} })); true;`
    )
  }, [])

  useWatchBridge(handleWatchMessage)

  return <BridgeWebView ref={webViewRef} style={styles.container} source={{ uri }} />
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
})
