import { useMemo } from 'react'
import { useSafeAreaInsets } from 'react-native-safe-area-context'
import Constants from 'expo-constants'

export default function useWebUri() {
  const { top, bottom } = useSafeAreaInsets()

  const uri = useMemo(() => {
    const host = Constants.expoConfig?.hostUri?.split(':')[0] || 'localhost'
    const baseURL = __DEV__
      ? `http://${host}:5173`
      : 'https://your-production-domain.com'
    return `${baseURL}?insetTop=${top}&insetBottom=${bottom}`
  }, [top, bottom])

  return { uri }
}
