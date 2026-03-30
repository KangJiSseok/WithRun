import { useEffect } from 'react'
import * as Location from 'expo-location'
import { NativeModules, Platform } from 'react-native'
import type { GpsLocation } from '../types'
import { createGpsKalmanFilter } from '../../utils/gpsKalmanFilter'

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

      // 구독 시작 시 새 필터 인스턴스 생성 (재구독 시 상태 초기화)
      const filterGps = createGpsKalmanFilter()

      subscription = await Location.watchPositionAsync(
        {
          accuracy: Location.Accuracy.BestForNavigation,
          timeInterval: 1000,
          distanceInterval: 0,
        },
        (location) => {
          const { latitude, longitude, accuracy, altitude, heading, speed } =
            location.coords
          const filtered = filterGps({
            latitude,
            longitude,
            accuracy: accuracy ?? 10,
            speed: speed ?? 0,
            heading: heading ?? 0,
            timestamp: location.timestamp ?? Date.now(),
          })
          onLocation({
            latitude: filtered.latitude,
            longitude: filtered.longitude,
            accuracy,
            altitude,
            heading,
            speedMps: speed,  // GPS Doppler 직접 측정 (가속도계 추정보다 정확)
            cadenceSpm: null, // 케이던스는 useCadence에서 별도 수집
          })

          if (Platform.OS === 'android') {
            const watchBridge = NativeModules.WatchBridge as
              | { sendGpsToWatch?: (payloadJson: string) => void }
              | undefined
            if (watchBridge?.sendGpsToWatch) {
              watchBridge.sendGpsToWatch(
                JSON.stringify({
                  latitude: filtered.latitude,
                  longitude: filtered.longitude,
                  accuracy: accuracy ?? null,
                  altitude: altitude ?? null,
                  heading: heading ?? null,
                  speedMps: speed ?? null,
                  timestamp: location.timestamp ?? Date.now(),
                })
              )
            }
          }
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
