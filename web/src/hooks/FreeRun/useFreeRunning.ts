// 러닝 데이터 훅
// 속도·거리: 모바일 GPS (Doppler 방식, 가속도계보다 정확)
// 칼로리·케이던스: 워치 Health Services (HR 기반 개인화 계산)
// 심박수: 워치 PPG + 모션보정 (모바일 불가)

import { useState, useEffect, useCallback, useRef } from 'react';
import { useBridgeDataStore } from '@/bridge';

// 두 GPS 좌표 간 거리 계산 (Haversine 공식)
function haversineKm(lat1: number, lon1: number, lat2: number, lon2: number): number {
  const R = 6371;
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLon = ((lon2 - lon1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos((lat1 * Math.PI) / 180) *
    Math.cos((lat2 * Math.PI) / 180) *
    Math.sin(dLon / 2) ** 2;
  return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

export function useFreeRunning() {
  const [isPaused, setIsPaused] = useState(false);
  const [time, setTime] = useState(0); // seconds
  const [distanceKm, setDistanceKm] = useState(0);

  const startTimeRef = useRef<number | null>(null);
  const elapsedRef = useRef(0);
  const prevGpsRef = useRef<{ lat: number; lon: number } | null>(null);

  const gpsLocation = useBridgeDataStore((s) => s.gpsLocation);
  const watchHealthData = useBridgeDataStore((s) => s.watchHealthData);

  // 타이머
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

  // 일시정지 해제 시 GPS 이전 좌표 초기화 (재개 시 점프 방지)
  useEffect(() => {
    if (!isPaused) {
      prevGpsRef.current = null;
    }
  }, [isPaused]);

  // GPS 거리 누적
  useEffect(() => {
    if (isPaused || !gpsLocation) return;

    const { latitude: lat, longitude: lon } = gpsLocation;

    if (prevGpsRef.current) {
      const delta = haversineKm(prevGpsRef.current.lat, prevGpsRef.current.lon, lat, lon);
      // GPS 노이즈 필터: 2m 미만(정지 노이즈) 또는 50m 초과(GPS 점프) 무시
      if (delta > 0.002 && delta < 0.05) {
        setDistanceKm((prev) => prev + delta);
      }
    }

    prevGpsRef.current = { lat, lon };
  }, [gpsLocation, isPaused]);

  // 속도: GPS Doppler 직접 측정값 (모바일)
  const speedMps = gpsLocation?.speedMps ?? 0;

  // 페이스: GPS 속도 기반 (0.5 m/s = ~1.8 km/h 이하는 정지로 간주)
  const pace =
    speedMps > 0.5
      ? (() => {
          const secPerKm = 1000 / speedMps;
          const mins = Math.floor(secPerKm / 60);
          const secs = Math.floor(secPerKm % 60);
          return `${mins}'${String(secs).padStart(2, '0')}"`;
        })()
      : `--'--"`;

  // 칼로리: Health Services 누적값 (HR 기반 개인화 계산 — 가장 정확)
  const calories = Math.round(watchHealthData?.caloriesKcal ?? 0);

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
    distance: distanceKm.toFixed(2),
    pace,
    calories,
    isPaused,
    togglePause,
  };
}
