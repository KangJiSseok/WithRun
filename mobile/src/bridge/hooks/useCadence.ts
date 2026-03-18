import { useEffect, useRef } from 'react'
import { Accelerometer } from 'expo-sensors'

// 50Hz 샘플링 — 달리기 케이던스(150~200 spm) 감지에 충분
const SAMPLE_INTERVAL_MS = 20
// 두 발걸음 사이 최소 간격 (250ms = 최대 ~240 spm)
const MIN_STEP_GAP_MS = 250
// 케이던스 계산 윈도우: 최근 5초
const WINDOW_MS = 5000
// 캘리브레이션 윈도우: 처음 0.5초(25샘플)의 평균 크기를 베이스라인으로 측정
const CALIBRATION_SAMPLES = 25
// 베이스라인 대비 이 비율 이상 상승 시 피크로 인식
// 정지 시 크기≈기준값, 달리기 충격≈1.3~2.0× → 1.08이면 가벼운 조깅도 감지
const PEAK_RATIO = 1.08

export function useCadence(onCadence: (spm: number) => void) {
  const stepTimesRef = useRef<number[]>([])
  const lastAboveRef = useRef(false)
  const lastStepAtRef = useRef(0)

  // 동적 베이스라인: 처음 CALIBRATION_SAMPLES개의 크기 평균값
  // → 기기별 가속도계 스케일(m/s² vs g 단위 혼재)에 무관하게 동작
  const calibSamplesRef = useRef<number[]>([])
  const baselineRef = useRef<number | null>(null)

  useEffect(() => {
    Accelerometer.setUpdateInterval(SAMPLE_INTERVAL_MS)

    const subscription = Accelerometer.addListener(({ x, y, z }) => {
      const magnitude = Math.sqrt(x * x + y * y + z * z)

      // 베이스라인 캘리브레이션 (초기 CALIBRATION_SAMPLES개 수집)
      if (baselineRef.current === null) {
        calibSamplesRef.current.push(magnitude)
        if (calibSamplesRef.current.length >= CALIBRATION_SAMPLES) {
          const sum = calibSamplesRef.current.reduce((a, b) => a + b, 0)
          baselineRef.current = sum / calibSamplesRef.current.length
        }
        return // 캘리브레이션 완료 전에는 스텝 감지 스킵
      }

      const threshold = baselineRef.current * PEAK_RATIO
      const isAbove = magnitude > threshold

      // 상승 엣지(rising edge)에서만 발걸음 카운트
      if (isAbove && !lastAboveRef.current) {
        const now = Date.now()
        if (now - lastStepAtRef.current >= MIN_STEP_GAP_MS) {
          lastStepAtRef.current = now
          stepTimesRef.current.push(now)
          // 윈도우 밖 데이터 제거
          const cutoff = now - WINDOW_MS
          stepTimesRef.current = stepTimesRef.current.filter((t) => t > cutoff)
          // spm = 윈도우 내 발걸음 수 × (60000 / 윈도우ms)
          const spm = Math.round(stepTimesRef.current.length * (60000 / WINDOW_MS))
          onCadence(spm)
        }
      }

      lastAboveRef.current = isAbove
    })

    return () => {
      subscription.remove()
      // 다음 마운트 시 재캘리브레이션
      calibSamplesRef.current = []
      baselineRef.current = null
      stepTimesRef.current = []
      lastAboveRef.current = false
      lastStepAtRef.current = 0
    }
  }, [onCadence])
}
