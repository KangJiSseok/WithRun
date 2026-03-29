interface GpsCoords {
  latitude: number
  longitude: number
  accuracy: number  // 단위: m
  speed: number     // 단위: m/s
  heading: number   // 단위: deg (현재 단순 버전에선 직접 사용하지 않음)
  timestamp: number // 단위: ms
}

interface FilteredCoords {
  latitude: number
  longitude: number
}

const MIN_ACCURACY_M = 3
const MAX_ACCURACY_M = 60
const MIN_ALPHA = 0.08
const MAX_ALPHA = 0.7

function clamp(value: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, value))
}

function haversineM(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 6371000
  const dLat = ((lat2 - lat1) * Math.PI) / 180
  const dLon = ((lon2 - lon1) * Math.PI) / 180
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLon / 2) ** 2
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

/**
 * 단순 GPS 보정기 (적응형 EMA)
 *
 * 왜 단순화했나?
 * - 복잡한 칼만 행렬보다 유지보수가 쉽고 튜닝 포인트가 직관적임
 * - accuracy/speed/timestamp를 실제 가중치에 반영해서
 *   고정 가중치보다 현장 노이즈 대응력이 좋음
 *
 * 동작 요약
 * 1) 정확도(accuracy)가 좋을수록 현재 측정값 비중(alpha)을 키움
 * 2) 이동 속도(speed)가 빠를수록 alpha를 추가로 키워 반응성 확보
 * 3) 정지 상태에서 급점프(outlier)는 alpha를 크게 줄여 흔들림 억제
 */
export function createGpsKalmanFilter() {
  let initialized = false
  let filteredLat = 0
  let filteredLng = 0
  let lastTimestamp = 0

  return function filter(coords: GpsCoords): FilteredCoords {
    const accuracy = clamp(coords.accuracy || 10, MIN_ACCURACY_M, MAX_ACCURACY_M)
    const speed = Math.max(0, coords.speed || 0)
    const timestamp = coords.timestamp || Date.now()

    // 첫 샘플은 그대로 초기값 사용
    if (!initialized) {
      initialized = true
      filteredLat = coords.latitude
      filteredLng = coords.longitude
      lastTimestamp = timestamp
      return { latitude: filteredLat, longitude: filteredLng }
    }

    // 정확도 기반 기본 가중치: 정확도가 좋을수록 alpha 증가
    const alphaByAccuracy = clamp(12 / accuracy, MIN_ALPHA, 0.55)

    // 속도 기반 가중치: 이동 중일수록 더 빨리 따라가도록 alpha 증가
    const alphaBySpeed = clamp(speed / 8, 0, 0.2)

    let alpha = clamp(alphaByAccuracy + alphaBySpeed, MIN_ALPHA, MAX_ALPHA)

    // 콜백 간격이 길어졌으면(백그라운드 복귀 등) 빠르게 재동기화
    const dtSec = Math.max(0, (timestamp - lastTimestamp) / 1000)
    if (dtSec > 2.5) {
      alpha = Math.max(alpha, 0.35)
    }
    lastTimestamp = timestamp

    // 정지 상태 급점프 억제: 정지인데 튀는 샘플은 덜 반영
    const jumpM = haversineM(filteredLat, filteredLng, coords.latitude, coords.longitude)
    const isStationary = speed < 0.7
    const jumpThresholdM = accuracy * 1.8 + 8
    if (isStationary && jumpM > jumpThresholdM) {
      alpha *= 0.15
    }

    filteredLat = filteredLat + alpha * (coords.latitude - filteredLat)
    filteredLng = filteredLng + alpha * (coords.longitude - filteredLng)

    return { latitude: filteredLat, longitude: filteredLng }
  }
}
