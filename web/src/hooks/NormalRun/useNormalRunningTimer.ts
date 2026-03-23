// NormalRun 세션 타이머 훅
// 책임: 일시정지 인식 타이머 (elapsed time 누적)

import { useState, useEffect, useCallback, useRef } from 'react';

import { formatTime } from '@/shared/utils';

export function useNormalRunningTimer() {
  const [isPaused, setIsPaused] = useState(false);
  const [time, setTime] = useState(0); // seconds

  const startTimeRef = useRef<number | null>(null);
  const elapsedRef = useRef(0);

  useEffect(() => {
    if (isPaused) {
      if (startTimeRef.current !== null) {
        elapsedRef.current += Date.now() - startTimeRef.current;
        startTimeRef.current = null;
      }
      return;
    }

    startTimeRef.current = Date.now();

    const interval = setInterval(() => {
      if (startTimeRef.current === null) return;
      setTime(Math.floor((elapsedRef.current + (Date.now() - startTimeRef.current)) / 1000));
    }, 1000);

    return () => clearInterval(interval);
  }, [isPaused]);

  const togglePause = useCallback(() => setIsPaused((prev) => !prev), []);

  return {
    durationSec: time,
    formattedTime: formatTime(time),
    isPaused,
    togglePause,
  };
}
