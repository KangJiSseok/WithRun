// NormalRun 세션 오케스트레이터 훅
// 책임: 서브 훅 조합 + 표시값 계산 + 세션 데이터 최종 종합(getCollectedData)
//
// 훅 간 결합도 = 0: 서브 훅끼리는 서로 import하지 않음
// 수직 결합(이 훅 → 서브 훅)만 존재
//
// 데이터 우선순위:
//   워치 연결 중 → 워치(Samsung Health) 데이터 우선
//   워치 미연결/끊김 → 모바일(GPS + 가속도계) 폴백

import { useCallback } from 'react';

import { useBridgeDataStore } from '@/bridge/stores/useBridgeDataStore';

import { useRunningTimer } from '@/shared/hooks/Running/useRunningTimer';
import { useRunningGps } from '@/shared/hooks/Running/useRunningGps';
import { useRunningHealth } from '@/shared/hooks/Running/useRunningHealth';
import { useRunningSplits } from '@/shared/hooks/Running/useRunningSplits';
import { useNormalRunCourseDeviation } from './useNormalRunCourseDeviation';
import type { CoordinatePoint } from '@/apis/course';

interface UseNormalRunningParams {
  courseCoordinates: CoordinatePoint[];
  onForceStop: () => void;
}

export function useNormalRunning({ courseCoordinates, onForceStop }: UseNormalRunningParams) {
  const { durationSec, formattedTime, isPaused, togglePause } = useRunningTimer();

  const { isDeviating, deviationSec } = useNormalRunCourseDeviation({
    courseCoordinates,
    isPaused,
    onForceStop,
  });

  // 이탈 중에도 데이터 수집 중단 (코스 데이터 오염 방지)
  const shouldPauseCollection = isPaused || isDeviating;

  const { haversineDistanceKm, elevationGainMRef, gpsSamplesRef } =
    useRunningGps({ isPaused: shouldPauseCollection });

  const { isWatchConnected, watchHealthData, healthSamplesRef, currentKmHeartRatesRef } =
    useRunningHealth({ isPaused: shouldPauseCollection });

  // 표시값 계산용: GPS Doppler 속도·케이던스 (데이터 수집은 GPS 훅에서 담당)
  const gpsLocation = useBridgeDataStore((s) => s.gpsLocation);

  // distanceKm: 워치 연결 시 워치 데이터 우선, 미연결 시 Haversine 폴백
  const distanceKm =
    isWatchConnected && watchHealthData && watchHealthData.distanceM > 0
      ? watchHealthData.distanceM / 1000
      : haversineDistanceKm;

  const { splitsRef } = useRunningSplits({
    distanceKm,
    durationSec,
    isPaused: shouldPauseCollection,
    elevationGainMRef,
    currentKmHeartRatesRef,
  });

  // ── 표시값 계산 ────────────────────────────────────────────────

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

  // 세션 종료 시 호출 — 수집된 샘플 배열과 종합 통계를 반환
  const getCollectedData = useCallback(() => {
    const distanceM = Math.round(distanceKm * 1000);
    const avgSpeedMps = durationSec > 0 ? distanceM / durationSec : 0;
    const avgPaceSecPerKm = distanceKm > 0 ? Math.round(durationSec / distanceKm) : 0;

    return {
      durationSec,
      distanceM,
      avgSpeedMps: parseFloat(avgSpeedMps.toFixed(2)),
      avgPaceSecPerKm,
      caloriesKcal: calories,
      elevationGainM: Math.round(elevationGainMRef.current),
      gpsSamples: gpsSamplesRef.current,
      healthSamples: healthSamplesRef.current,
      splits: splitsRef.current,
    };
  }, [durationSec, distanceKm, calories, elevationGainMRef, gpsSamplesRef, healthSamplesRef, splitsRef]);

  return {
    time: formattedTime,
    durationSec,
    distance: distanceKm.toFixed(2),
    pace,
    calories,
    heartRate,
    cadenceSpm,
    isWatchConnected,
    isPaused,
    togglePause,
    getCollectedData,
    isDeviating,
    deviationSec,
  };
}
