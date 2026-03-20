// NormalRun 세션 러닝 데이터 훅
// TODO: 코스 완주 감지 로직 추가 예정
//
// 데이터 우선순위:
//   워치 연결 중 → 워치(Samsung Health) 데이터 우선
//   워치 미연결/끊김 → 모바일(GPS + 가속도계) 폴백
//
// 워치 담당: HR, 칼로리, 케이던스, 거리 (Samsung Health 기반)
// 모바일 담당: 위도·경도·고도·방향·정확도·속도 (GPS Doppler 직접 측정)
// 모바일 폴백: 케이던스(가속도계), 거리(Haversine)

import { useState, useEffect, useCallback, useRef } from 'react';

import { useBridgeDataStore } from '@/bridge/stores/useBridgeDataStore';
import type { GpsSample, HealthSample } from '@/apis/runningSessions';

// 워치 메시지가 이 시간(ms) 이상 오지 않으면 연결 끊김으로 판단
const WATCH_TIMEOUT_MS = 3000;

// 두 GPS 좌표 간 거리 계산 (Haversine 공식, 단위: km)
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

export function useNormalRunning() {
  const [isPaused, setIsPaused] = useState(false);
  const [time, setTime] = useState(0); // seconds
  const [haversineDistanceKm, setHaversineDistanceKm] = useState(0);
  const [isWatchConnected, setIsWatchConnected] = useState(false);

  const startTimeRef = useRef<number | null>(null);
  const elapsedRef = useRef(0);
  const prevGpsRef = useRef<{ lat: number; lon: number } | null>(null);
  const gpsSamplesRef = useRef<GpsSample[]>([]);
  const healthSamplesRef = useRef<HealthSample[]>([]);

  const gpsLocation = useBridgeDataStore((s) => s.gpsLocation);
  const watchHealthData = useBridgeDataStore((s) => s.watchHealthData);
  const lastWatchMessageAt = useBridgeDataStore((s) => s.lastWatchMessageAt);

  // 워치 연결 상태 1초마다 체크
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

  // GPS 거리 누적 (워치 폴백용으로 항상 계산)
  useEffect(() => {
    if (isPaused || !gpsLocation) return;

    const { latitude: lat, longitude: lon } = gpsLocation;

    if (prevGpsRef.current) {
      const delta = haversineKm(prevGpsRef.current.lat, prevGpsRef.current.lon, lat, lon);
      // GPS 노이즈 필터: 2m 미만(정지 노이즈) 또는 50m 초과(GPS 점프) 무시
      if (delta > 0.002 && delta < 0.05) {
        setHaversineDistanceKm((prev) => prev + delta);
      }
    }

    prevGpsRef.current = { lat, lon };

    // GPS 샘플 누적 (세션 종료 시 서버 전송용)
    gpsSamplesRef.current.push({
      latitude: gpsLocation.latitude,
      longitude: gpsLocation.longitude,
      altitudeM: gpsLocation.altitude ?? 0,
      accuracyM: gpsLocation.accuracy ?? 0,
      bearingDeg: gpsLocation.heading ?? 0,
      sampledAt: new Date().toISOString(),
    });
  }, [gpsLocation, isPaused]);

  // 헬스 샘플 누적 (세션 종료 시 서버 전송용)
  useEffect(() => {
    if (isPaused || !watchHealthData) return;
    const speedMps = gpsLocation?.speedMps ?? 0;
    healthSamplesRef.current.push({
      sampledAt: new Date(watchHealthData.timestamp).toISOString(),
      heartRate: watchHealthData.heartRate,
      speedMps,
      paceSecPerKm: speedMps > 0 ? Math.round(1000 / speedMps) : 0,
      distanceM: watchHealthData.distanceM,
      caloriesKcal: watchHealthData.caloriesKcal,
      cadenceSpm: isWatchConnected
        ? (watchHealthData.cadenceSpm ?? gpsLocation?.cadenceSpm ?? 0)
        : (gpsLocation?.cadenceSpm ?? 0),
    });
  }, [watchHealthData, isPaused]);

  // ── 출력값 계산 ────────────────────────────────────────────────

  const speedMps = gpsLocation?.speedMps ?? 0;

  const pace =
    speedMps > 0.5
      ? (() => {
          const secPerKm = 1000 / speedMps;
          const mins = Math.floor(secPerKm / 60);
          const secs = Math.floor(secPerKm % 60);
          return `${mins}'${String(secs).padStart(2, '0')}"`;
        })()
      : `--'--"`;

  const calories = Math.round(watchHealthData?.caloriesKcal ?? 0);
  const heartRate = watchHealthData?.heartRate ?? 0;

  const cadenceSpm = isWatchConnected
    ? (watchHealthData?.cadenceSpm ?? gpsLocation?.cadenceSpm ?? 0)
    : (gpsLocation?.cadenceSpm ?? 0);

  const distanceKm =
    isWatchConnected && watchHealthData && watchHealthData.distanceM > 0
      ? watchHealthData.distanceM / 1000
      : haversineDistanceKm;

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

  // 세션 종료 시 호출 — 수집된 샘플 배열과 종합 통계를 반환
  const getCollectedData = useCallback(() => {
    const distanceM = Math.round(distanceKm * 1000);
    const avgSpeedMps = time > 0 ? distanceM / time : 0;
    const avgPaceSecPerKm = distanceKm > 0 ? Math.round(time / distanceKm) : 0;

    return {
      durationSec: time,
      distanceM,
      avgSpeedMps: parseFloat(avgSpeedMps.toFixed(2)),
      avgPaceSecPerKm,
      caloriesKcal: calories,
      gpsSamples: gpsSamplesRef.current,
      healthSamples: healthSamplesRef.current,
    };
  }, [time, distanceKm, calories]);

  return {
    time: formatTime(time),
    durationSec: time,
    distance: distanceKm.toFixed(2),
    pace,
    calories,
    heartRate,
    cadenceSpm,
    isWatchConnected,
    isPaused,
    togglePause,
    getCollectedData,
  };
}
