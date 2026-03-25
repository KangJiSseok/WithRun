// 러닝 헬스 샘플 수집 훅 (FreeRun / NormalRun 공용)
// 책임: 워치 연결 감지 + 헬스 샘플 수집 + 구간 심박수 추적

import { useState, useEffect, useRef } from 'react';

import { useBridgeDataStore } from '@/bridge/stores/useBridgeDataStore';
import type { HealthSample } from '@/apis/runningSessions';

// 워치 메시지가 이 시간(ms) 이상 오지 않으면 연결 끊김으로 판단
const WATCH_TIMEOUT_MS = 3000;
export function useRunningHealth({ isPaused }: { isPaused: boolean }) {
  const [isWatchConnected, setIsWatchConnected] = useState(false);

  const healthSamplesRef = useRef<HealthSample[]>([]);
  const currentKmHeartRatesRef = useRef<number[]>([]);

  // latest-ref 패턴: isPaused 변경 시 effect 재실행 없이 최신값 읽기
  const latestIsPausedRef = useRef(isPaused);
  latestIsPausedRef.current = isPaused;

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

  // 헬스 샘플 누적 + 구간 심박수 추적
  // isPaused를 dep에서 제외(ref로 읽기): isPaused 변경 시 재실행으로 인한 중복 샘플 방지
  useEffect(() => {
    if (latestIsPausedRef.current || !watchHealthData) return;

    healthSamplesRef.current.push({
      sampledAt: new Date(watchHealthData.timestamp).toISOString(),
      heartRate: watchHealthData.heartRate,
      caloriesKcal: watchHealthData.caloriesKcal,
    });

    if (watchHealthData.heartRate > 0) {
      currentKmHeartRatesRef.current.push(watchHealthData.heartRate);
    }
  }, [watchHealthData]);

  return {
    isWatchConnected,
    watchHealthData,
    healthSamplesRef,
    currentKmHeartRatesRef,
  };
}
