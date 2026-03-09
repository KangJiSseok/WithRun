import React from 'react'
import { SafeAreaProvider } from 'react-native-safe-area-context'
import WebViewScreen from './screens/webview/WebViewScreen'

export default function App() {
  return (
    <SafeAreaProvider>
      <WebViewScreen />
    </SafeAreaProvider>
  )
}
