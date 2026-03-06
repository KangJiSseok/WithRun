import { useRef, useCallback, useEffect } from 'react'
import { Platform, StyleSheet, Text, View } from 'react-native'
import WebView from 'react-native-webview'
import type { WebViewMessageEvent } from 'react-native-webview'

import { WatchDebugOverlay } from '@/components/watch-debug-overlay'
import { useWatchNativeEvents } from '@/hooks/use-watch-native-events'
import type { WatchCommand, WebViewBridgeEnvelope } from '@/types/watch-bridge'

/** In dev, points to the Vite dev server running on the host machine.
 *  Set EXPO_PUBLIC_WEBVIEW_DEV_URL for physical devices.
 *  Example: EXPO_PUBLIC_WEBVIEW_DEV_URL=http://192.168.0.10:5173
 *  10.0.2.2 is the Android emulator alias for localhost.
 *  In production, use the bundled asset: file:///android_asset/watch-web/index.html
 */
const WEBVIEW_DEV_URL = process.env.EXPO_PUBLIC_WEBVIEW_DEV_URL?.trim()

const WEBVIEW_URL = __DEV__
  ? (WEBVIEW_DEV_URL && WEBVIEW_DEV_URL.length > 0
      ? WEBVIEW_DEV_URL
      : 'http://10.0.2.2:5173')
  : 'file:///android_asset/watch-web/index.html'

function buildInjectScript(envelope: WebViewBridgeEnvelope): string {
  const serialized = JSON.stringify(JSON.stringify(envelope))
  return `(function(){try{var d=${serialized};document.dispatchEvent(new MessageEvent('message',{data:d}));window.dispatchEvent(new MessageEvent('message',{data:d}));}catch(e){}})();true;`
}

export default function WatchScreen() {
  const webViewRef = useRef<WebView | null>(null)
  const { lastPayload, lastAck, connectionStatus, isConnected, sendCommand } =
    useWatchNativeEvents()

  useEffect(() => {
    if (webViewRef.current == null || lastPayload == null) return
    const envelope: WebViewBridgeEnvelope = {
      event: 'WearHealthUpdate',
      data: JSON.stringify(lastPayload),
    }
    webViewRef.current.injectJavaScript(buildInjectScript(envelope))
  }, [lastPayload])

  useEffect(() => {
    if (webViewRef.current == null || lastAck == null) return
    const envelope: WebViewBridgeEnvelope = {
      event: 'WearAckReceived',
      data: JSON.stringify(lastAck),
    }
    webViewRef.current.injectJavaScript(buildInjectScript(envelope))
  }, [lastAck])

  useEffect(() => {
    if (webViewRef.current == null || connectionStatus == null) return
    const envelope: WebViewBridgeEnvelope = {
      event: 'WearConnectionStatus',
      data: JSON.stringify(connectionStatus),
    }
    webViewRef.current.injectJavaScript(buildInjectScript(envelope))
  }, [connectionStatus])

  const onWebViewLoadEnd = useCallback(() => {
    // WebView 로드 완료 시 현재 connectionStatus를 다시 전송
    if (webViewRef.current != null && connectionStatus != null) {
      const envelope: WebViewBridgeEnvelope = {
        event: 'WearConnectionStatus',
        data: JSON.stringify(connectionStatus),
      }
      webViewRef.current.injectJavaScript(buildInjectScript(envelope))
    }
  }, [connectionStatus])

  const onWebViewMessage = useCallback(
    (event: WebViewMessageEvent) => {
      try {
        const command = JSON.parse(event.nativeEvent.data) as WatchCommand
        void sendCommand(command)
      } catch {
        // ignore malformed messages from web
      }
    },
    [sendCommand],
  )

  if (Platform.OS !== 'android') {
    return (
      <View style={styles.unsupported}>
        <Text style={styles.unsupportedText}>
          Watch integration requires Android with paired Galaxy Watch.
        </Text>
      </View>
    )
  }

  return (
    <View style={styles.container}>
      <WatchDebugOverlay
        isConnected={isConnected}
        lastPayloadTimestamp={lastPayload?.timestampMs ?? null}
        lastAckType={lastAck?.type ?? null}
        nodeName={connectionStatus?.nodeName ?? null}
      />
      <WebView
        ref={webViewRef}
        source={{ uri: WEBVIEW_URL }}
        onMessage={onWebViewMessage}
        onLoadEnd={onWebViewLoadEnd}
        style={styles.webView}
        originWhitelist={['*']}
        javaScriptEnabled
        domStorageEnabled
        mixedContentMode="compatibility"
        allowsInlineMediaPlayback
      />
    </View>
  )
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#1a1a2e',
  },
  webView: {
    flex: 1,
    backgroundColor: '#1a1a2e',
  },
  unsupported: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    padding: 24,
    backgroundColor: '#1a1a2e',
  },
  unsupportedText: {
    color: '#888',
    textAlign: 'center',
    fontSize: 14,
  },
})
