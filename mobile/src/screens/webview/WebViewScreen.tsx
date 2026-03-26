import { useCallback, useEffect, useRef, useState } from 'react'
import { Linking, StyleSheet } from 'react-native'
import { SafeAreaView } from 'react-native-safe-area-context'
import { WebView as BridgeWebView, postMessage, useWatchBridge, useGpsLocation, useCadence } from '@/bridge'
import type { GpsLocation, WatchHealthData } from '@/bridge'
import useWebUri from './hooks/useWebUri'

function resolveWebCallbackUri(baseUri: string, deepLinkUrl: string): string | null {
  // 네이티브 딥링크를 그대로 WebView에 넣지 않고, 웹이 이해하는 /auth/callback URL로 다시 바꿔준다.
  if (!deepLinkUrl.startsWith('withrun://auth/callback')) {
    return null
  }

  const normalizedBaseUri = baseUri.endsWith('/')
    ? baseUri.slice(0, -1)
    : baseUri
  const queryIndex = deepLinkUrl.indexOf('?')
  const queryString = queryIndex >= 0 ? deepLinkUrl.slice(queryIndex) : ''

  return `${normalizedBaseUri}/auth/callback${queryString}`
}

export default function WebViewScreen() {
  const { uri } = useWebUri()
  const cadenceSpmRef = useRef<number | null>(null)
  const [currentUri, setCurrentUri] = useState(uri)

  const handleDeepLink = useCallback(
    (deepLinkUrl: string) => {
      const callbackUri = resolveWebCallbackUri(uri, deepLinkUrl)

      if (!callbackUri) {
        return
      }

      setCurrentUri(callbackUri)
    },
    [uri],
  )

  useEffect(() => {
    setCurrentUri(uri)
  }, [uri])

  useEffect(() => {
    // cold start 로 들어온 딥링크와 런타임 딥링크를 둘 다 처리해서 OAuth 복귀를 놓치지 않는다.
    const subscription = Linking.addEventListener('url', ({ url }) => {
      handleDeepLink(url)
    })

    void Linking.getInitialURL().then((initialUrl) => {
      if (initialUrl) {
        handleDeepLink(initialUrl)
      }
    })

    return () => {
      subscription.remove()
    }
  }, [handleDeepLink])

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
      <BridgeWebView style={styles.container} source={{ uri: currentUri }} />
    </SafeAreaView>
  )
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
  },
})
