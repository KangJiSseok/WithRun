// 러닝 데이터 훅
//
// 데이터 우선순위:
//   워치 연결 중 → 워치(Samsung Health) 데이터 우선
//   워치 미연결/끊김 → 모바일(GPS + 가속도계) 폴백
//
// 워치 담당: HR, 칼로리, 케이던스, 거리 (Samsung Health 기반)
// 모바일 담당: 위도·경도·고도·방향·정확도·속도 (GPS Doppler 직접 측정)
// 모바일 폴백: 케이던스(가속도계), 거리(Haversine)

import { useState, useEffect, useCallback, useRef } from 'react';

import { useBridgeDataStore } from '@/bridge';
import { haversineKm, formatTime } from '@/shared/utils';

// 워치 메시지가 이 시간(ms) 이상 오지 않으면 연결 끊김으로 판단
const WATCH_TIMEOUT_MS = 3000;

export function useFreeRunning() {
  const [isPaused, setIsPaused] = useState(false);
  const [time, setTime] = useState(0); // seconds
  // GPS 기반 누적 거리 (워치 폴백용으로 항상 계산)
  const [haversineDistanceKm, setHaversineDistanceKm] = useState(0);
  // 워치 연결 여부 (렌더 트리거용)
  const [isWatchConnected, setIsWatchConnected] = useState(false);

  const startTimeRef = useRef<number | null>(null);
  const elapsedRef = useRef(0);
  const prevGpsRef = useRef<{ lat: number; lon: number } | null>(null);

  const gpsLocation = useBridgeDataStore((s) => s.gpsLocation);
  const watchHealthData = useBridgeDataStore((s) => s.watchHealthData);
  const lastWatchMessageAt = useBridgeDataStore((s) => s.lastWatchMessageAt);

  // 워치 연결 상태를 1초마다 체크 (lastWatchMessageAt 기반)
  useEffect(() => {
    const check = () => {
      const connected =
        lastWatchMessageAt !== null &&
        Date.now() - lastWatchMessageAt < WATCH_TIMEOUT_MS;
      setIsWatchConnected(connected);
    };
    check();
    const interval = setInterval(check, 1000);
    return () => clearInterval(interval);
  }, [lastWatchMessageAt]);

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

  // latest-ref 패턴: isPaused 변경 시 GPS effect 재실행으로 인한 중복 샘플 방지
  const latestIsPausedRef = useRef(isPaused);
  latestIsPausedRef.current = isPaused;

  // GPS 거리 누적 (워치 폴백용으로 항상 계산)
  // isPaused를 dep에서 제외(ref로 읽기): isPaused 변경 시 재실행으로 인한 중복 샘플 방지
  useEffect(() => {
    if (latestIsPausedRef.current || !gpsLocation) return;

    const { latitude: lat, longitude: lon } = gpsLocation;

    if (prevGpsRef.current) {
      const delta = haversineKm(prevGpsRef.current.lat, prevGpsRef.current.lon, lat, lon);
      // GPS 노이즈 필터: 2m 미만(정지 노이즈) 또는 50m 초과(GPS 점프) 무시
      if (delta > 0.002 && delta < 0.05) {
        setHaversineDistanceKm((prev) => prev + delta);
      }
    }

    prevGpsRef.current = { lat, lon };
  }, [gpsLocation]);

  // ── 출력값 계산 ────────────────────────────────────────────────

  // 속도: GPS Doppler 직접 측정 (워치/모바일 공통으로 모바일 GPS가 항상 우세)
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

  // 칼로리: 워치 Health Services 누적값 (워치 전용, 모바일 불가)
  const calories = Math.round(watchHealthData?.caloriesKcal ?? 0);

  // 심박수: 워치 PPG 전용 (모바일 불가)
  const heartRate = watchHealthData?.heartRate ?? 0;

  // 케이던스: 워치 연결 시 Samsung Health SPM 우선, 미연결 시 모바일 가속도계 폴백
  const cadenceSpm = isWatchConnected
    ? (watchHealthData?.cadenceSpm ?? gpsLocation?.cadenceSpm ?? 0)
    : (gpsLocation?.cadenceSpm ?? 0);

  // 거리: 워치 연결 시 Samsung Health 만보계 거리 우선, 미연결 시 Haversine 폴백
  const distanceKm = isWatchConnected && watchHealthData && watchHealthData.distanceM > 0
    ? watchHealthData.distanceM / 1000
    : haversineDistanceKm;

  const togglePause = useCallback(() => setIsPaused((prev) => !prev), []);

  return {
    time: formatTime(time),
    distance: distanceKm.toFixed(2),
    pace,
    calories,
    heartRate,
    cadenceSpm,
    isWatchConnected,
    isPaused,
    togglePause,
  };
}
