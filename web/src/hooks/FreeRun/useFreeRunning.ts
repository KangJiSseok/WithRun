// 타이머 + 거리/페이스/칼로리 목업 훅
// 추후 거리, 페이스, 칼로리는 실제 센서 데이터로 대체 예정

import { useState, useEffect, useCallback, useRef } from 'react';

export function useFreeRunning() {
  const [isPaused, setIsPaused] = useState(false);
  const [time, setTime] = useState(0); // seconds
  const [distance, setDistance] = useState(0); // km (mock)

  const startTimeRef = useRef<number | null>(null); // 시작 시각 (ms)
  const elapsedRef = useRef(0); // 일시정지 전까지 누적 경과 시간 (ms)

  useEffect(() => {
    if (isPaused) {
      // 일시정지 시점에 경과 시간 저장
      if (startTimeRef.current !== null) {
        elapsedRef.current += Date.now() - startTimeRef.current;
        startTimeRef.current = null;
      }
      return;
    }

    startTimeRef.current = Date.now();

    const interval = setInterval(() => {
      if (startTimeRef.current === null) return;
      const totalElapsedSec = Math.floor(
        (elapsedRef.current + (Date.now() - startTimeRef.current)) / 1000
      );
      setTime(totalElapsedSec);
      setDistance(+(totalElapsedSec * 0.003).toFixed(3)); // ~10.8 km/h 목업
    }, 1000);

    return () => clearInterval(interval);
  }, [isPaused]);

  const pace =
    distance > 0
      ? (() => {
          const secPerKm = time / distance;
          const mins = Math.floor(secPerKm / 60);
          const secs = Math.floor(secPerKm % 60);
          return `${mins}'${String(secs).padStart(2, '0')}"`;
        })()
      : `--'--"`;

  const calories = Math.round(distance * 60);

  const formatTime = (s: number) => {
    const h = Math.floor(s / 3600);
    const m = Math.floor((s % 3600) / 60);
    const sec = s % 60;
    if (h > 0) {
      return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`;
    }
    return `${String(m).padStart(2, '0')}:${String(sec).padStart(2, '0')}`;
  };

  const togglePause = useCallback(() => setIsPaused((prev) => !prev), []);

  return {
    time: formatTime(time),
    distance: distance.toFixed(2),
    pace,
    calories,
    isPaused,
    togglePause,
  };
}
