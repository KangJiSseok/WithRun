import { useEffect } from 'react'
import * as Location from 'expo-location'

type GpsLocation = {
  latitude: number
  longitude: number
  accuracy: number | null
}

export default function useGpsLocation(onLocation: (location: GpsLocation) => void) {
  useEffect(() => {
    let subscription: Location.LocationSubscription | null = null

    async function start() {
      const { status } = await Location.requestForegroundPermissionsAsync()
      if (status !== 'granted') {
        console.warn('[GPS] 위치 권한이 거부되었습니다.')
        return
      }

      subscription = await Location.watchPositionAsync(
        {
          accuracy: Location.Accuracy.High,
          timeInterval: 1000,
          distanceInterval: 1,
        },
        (location) => {
          const { latitude, longitude, accuracy } = location.coords
          onLocation({ latitude, longitude, accuracy })
        },
      )
    }

    start()

    return () => {
      subscription?.remove()
    }
  }, [onLocation])
}
