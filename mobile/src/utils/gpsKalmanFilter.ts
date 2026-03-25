import KalmanFilter from 'kalmanjs';

interface GpsCoords {
  latitude: number
  longitude: number
  accuracy: number  // 단위: m
  speed: number
  heading: number
}

interface FilteredCoords {
  latitude: number
  longitude: number
}

/**
 * GPS 칼만 필터 팩토리 함수 (kalmanjs 기반)
 *
 * @param Q - 프로세스 노이즈 (기본값 1)
 *   - Q가 클수록 새 GPS 측정값을 더 신뢰 → 반응 빠르지만 노이즈 많음
 *   - Q가 작을수록 이전 추정값을 더 신뢰 → 부드럽지만 반응 느림
 *   - 러닝: Q = 3 권장 (빠른 이동, 반응성 중요)
 *   - 걷기: Q = 1 권장 (느린 이동, 안정성 중요)
 *
 * @returns GPS 좌표를 받아 필터링된 좌표를 반환하는 함수
 *
 * @example
 * const filterGps = createGpsKalmanFilter(3)
 * const filtered = filterGps({ latitude, longitude, accuracy, speed, heading })
 */
export function createGpsKalmanFilter(Q = 1) {
  const filterLat = new KalmanFilter({ R: 1, Q })
  const filterLng = new KalmanFilter({ R: 1, Q })

  return function filter(coords: GpsCoords): FilteredCoords {
    return {
      latitude: filterLat.filter(coords.latitude),
      longitude: filterLng.filter(coords.longitude),
    }
  }
}
