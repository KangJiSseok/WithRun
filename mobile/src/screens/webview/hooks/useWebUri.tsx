import { useMemo } from 'react'
import { useSafeAreaInsets } from 'react-native-safe-area-context'
import Constants from 'expo-constants'
import { Platform } from 'react-native'

export default function useWebUri() {
  const { top, bottom } = useSafeAreaInsets()

  const uri = useMemo(() => {
    const androidHost = process.env.EXPO_PUBLIC_WEB_HOST
    const webHost = Constants.expoConfig?.hostUri?.split(':')[0] || 'localhost'
    const baseURL = (() => {
      // 실배포
      if (!__DEV__) return 'https://your-production-domain.com'
      if (Platform.OS === 'android')
        return `http://${androidHost || 'localhost'}:5173` // android
      else if (Platform.OS === 'ios')
        return `http://${androidHost || 'localhost'}:5173` // ios(임시)
      else return `http://${webHost}:5173` // web
    })()

    return `${baseURL}?insetTop=${top}&insetBottom=${bottom}`
  }, [top, bottom])

  return { uri }
}
