// NormalRun 코스 이탈 감지 훅
// 책임: GPS 비교 → 이탈 감지 → 카운터 → 브릿지 알림 → 강제 종료 신호
//
// 동작 명세:
//   GPS 갱신 + 코스와 거리 > 30m + !isPaused → isDeviating = true
//   GPS 갱신 + 코스와 거리 ≤ 30m            → isDeviating = false, deviationSec = 0
//   isDeviating false→true + !isPaused       → bridge.notifyCourseDeviation(1) (진동 1회) + toast 경고
//   deviationSec === 4                        → bridge.notifyCourseDeviation(2) (진동 2회) + toast 주의
//   deviationSec === 8                        → bridge.notifyCourseDeviation(3) (진동 3회) + toast 위험
//   deviationSec >= 10                        → bridge.notifyCourseDeviationForceStop() + toast + onForceStop() 호출
//   isPaused 중                               → 카운터 증가 없음

import { useEffect, useRef, useState } from 'react';
import { toast } from 'sonner';

import { bridge } from '@/bridge';
import { useBridgeDataStore } from '@/bridge/stores/useBridgeDataStore';
import type { CoordinatePoint } from '@/apis/course';
import { pointToPolylineM } from '@/shared/utils/geo';

const DEVIATION_THRESHOLD_M = 30;
const FORCE_STOP_SEC = 10;

interface UseNormalRunCourseDeviationParams {
  courseCoordinates: CoordinatePoint[];
  isPaused: boolean;
  onForceStop: () => void;
}

export function useNormalRunCourseDeviation({
  courseCoordinates,
  isPaused,
  onForceStop,
}: UseNormalRunCourseDeviationParams): {
  isDeviating: boolean;
  deviationSec: number;
} {
  const gpsLocation = useBridgeDataStore((s) => s.gpsLocation);

  const [isDeviating, setIsDeviating] = useState(false);
  const [deviationSec, setDeviationSec] = useState(0);

  // interval 내부에서 최신값 접근용 ref
  const deviationSecRef = useRef(0);
  const latestIsPausedRef = useRef(isPaused);
  latestIsPausedRef.current = isPaused;
  const latestOnForceStopRef = useRef(onForceStop);
  latestOnForceStopRef.current = onForceStop;

  // GPS 갱신 시 이탈 여부 판단
  useEffect(() => {
    if (!gpsLocation || isPaused) return;

    const dist = pointToPolylineM(gpsLocation.latitude, gpsLocation.longitude, courseCoordinates);
    const deviating = dist > DEVIATION_THRESHOLD_M;

    setIsDeviating((prev) => {
      // false → true 전환: 1차 알림 (진동 1회)
      if (!prev && deviating) {
        bridge.notifyCourseDeviation(1).catch(() => {});
        toast.warning('코스를 이탈했습니다', {
          description: '10초 내에 코스로 복귀하지 않으면 세션이 종료됩니다.',
          duration: 4000,
        });
      }
      // true → false 전환: 카운터 리셋
      if (prev && !deviating) {
        deviationSecRef.current = 0;
        setDeviationSec(0);
        toast.success('코스로 복귀했습니다');
      }
      return deviating;
    });
  // courseCoordinates는 렌더 간 참조 변경이 없다고 가정 (memoized)
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [gpsLocation, isPaused]);

  // 이탈 중 1초 카운터
  useEffect(() => {
    if (!isDeviating) return;

    const intervalId = setInterval(() => {
      if (latestIsPausedRef.current) return;

      deviationSecRef.current += 1;
      const next = deviationSecRef.current;
      setDeviationSec(next);

      if (next === 4) {
        bridge.notifyCourseDeviation(2).catch(() => {}); // 진동 2회
        toast.error('코스 이탈 6초 남음', {
          description: '빠르게 코스로 복귀해주세요.',
          duration: 3000,
        });
      } else if (next === 8) {
        bridge.notifyCourseDeviation(3).catch(() => {}); // 진동 3회
        toast.error('코스 이탈 2초 남음', {
          description: '세션이 곧 종료됩니다!',
          duration: 3000,
        });
      }

      if (next >= FORCE_STOP_SEC) {
        clearInterval(intervalId);
        bridge.notifyCourseDeviationForceStop().catch(() => {}); // 강제 종료 진동 1000ms
        toast.error('코스를 이탈해 러닝을 종료합니다.', { duration: 3000 });
        latestOnForceStopRef.current();
      }
    }, 1000);

    return () => clearInterval(intervalId);
  }, [isDeviating]);

  return { isDeviating, deviationSec };
}
