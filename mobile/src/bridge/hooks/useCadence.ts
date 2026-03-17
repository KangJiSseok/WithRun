import { useEffect, useRef } from 'react'
import { Accelerometer } from 'expo-sensors'

// 50Hz 샘플링 — 달리기 케이던스(150~200 spm) 감지에 충분
const SAMPLE_INTERVAL_MS = 20
// 중력(~9.81 m/s²) 대비 1.5 m/s² 초과 시 피크로 인식
const STEP_THRESHOLD = 11.3
// 두 발걸음 사이 최소 간격 (250ms = 최대 ~240 spm)
const MIN_STEP_GAP_MS = 250
// 케이던스 계산 윈도우: 최근 5초
const WINDOW_MS = 5000

export function useCadence(onCadence: (spm: number) => void) {
  const stepTimesRef = useRef<number[]>([])
  const lastAboveRef = useRef(false)
  const lastStepAtRef = useRef(0)

  useEffect(() => {
    Accelerometer.setUpdateInterval(SAMPLE_INTERVAL_MS)

    const subscription = Accelerometer.addListener(({ x, y, z }) => {
      const magnitude = Math.sqrt(x * x + y * y + z * z)
      const isAbove = magnitude > STEP_THRESHOLD

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

    return () => subscription.remove()
  }, [onCadence])
}
