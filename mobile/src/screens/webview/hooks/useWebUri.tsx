import { useMemo } from 'react'
import Constants from 'expo-constants'
import { Platform } from 'react-native'

export default function useWebUri() {
  const uri = useMemo(() => {
    const androidHost = process.env.EXPO_PUBLIC_WEB_HOST
    const webHost = Constants.expoConfig?.hostUri?.split(':')[0] || 'localhost'
    const baseURL = (() => {
      // 실배포
      if (!__DEV__) return process.env.EXPO_PUBLIC_PROD_WEB_DNS
      if (Platform.OS === 'android')
        return `http://${androidHost || 'localhost'}:5173` // android
      else if (Platform.OS === 'ios')
        return `http://${androidHost || 'localhost'}:5173` // ios(임시)
      else return `http://${webHost}:5173` // web
    })()

    return baseURL
  }, [])

  return { uri }
}
