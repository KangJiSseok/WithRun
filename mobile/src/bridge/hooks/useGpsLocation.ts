import { useEffect } from 'react'
import * as Location from 'expo-location'

export type GpsLocation = {
  latitude: number
  longitude: number
  accuracy: number | null
  altitude: number | null
  heading: number | null
}

export function useGpsLocation(onLocation: (location: GpsLocation) => void) {
  useEffect(() => {
    let cancelled = false
    let subscription: Location.LocationSubscription | null = null

    async function start() {
      const { status } = await Location.requestForegroundPermissionsAsync()
      if (cancelled) return
      if (status !== 'granted') {
        console.warn('[GPS] 위치 권한이 거부되었습니다.')
        return
      }

      subscription = await Location.watchPositionAsync(
        {
          accuracy: Location.Accuracy.High,
          timeInterval: 1000,
          distanceInterval: 0, // 위치 변화 없어도 1초마다 업데이트
        },
        (location) => {
          const { latitude, longitude, accuracy, altitude, heading } =
            location.coords
          onLocation({
            latitude,
            longitude,
            accuracy,
            altitude,
            heading,
          })
        }
      )

      // 비동기 대기 중에 cleanup이 먼저 실행된 경우 즉시 제거
      if (cancelled) {
        subscription.remove()
        subscription = null
      }
    }

    start()

    return () => {
      cancelled = true
      subscription?.remove()
    }
  }, [onLocation])
}
