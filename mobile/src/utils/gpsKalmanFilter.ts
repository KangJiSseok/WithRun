interface GpsCoords {
  latitude: number
  longitude: number
  accuracy: number  // 단위: m → 측정 노이즈 R = accuracy²
  speed: number
  heading: number
}

interface FilteredCoords {
  latitude: number
  longitude: number
}

/**
 * GPS 칼만 필터 팩토리 함수
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
  let P = 0
  let prevLat = 0
  let prevLng = 0
  let initialized = false

  return function filter(coords: GpsCoords): FilteredCoords {
    const { latitude, longitude, accuracy } = coords
    const R = accuracy * accuracy

    if (!initialized) {
      initialized = true
      prevLat = latitude
      prevLng = longitude
      return { latitude, longitude }
    }

    // 예측 단계: 불확실성 증가
    P += Q

    // 칼만 게인: R이 클수록(GPS 신뢰도 낮을수록) 측정값 가중치 감소
    const K = P / (P + R)

    // 보정 단계: 이전 추정값과 새 측정값의 가중 평균
    prevLat += K * (latitude - prevLat)
    prevLng += K * (longitude - prevLng)

    // 불확실성 업데이트
    P *= (1 - K)

    return { latitude: prevLat, longitude: prevLng }
  }
}
