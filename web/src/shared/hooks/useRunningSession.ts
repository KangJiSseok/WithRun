import { useEffect, useRef, useState } from 'react';
import { useBridgeDataStore } from '@/bridge';
import { createRunningSession } from '@/apis';

// 회원 시스템 구현 전 임시 mock 유저
const MOCK_USER_ID = 1;

// Bridge GPS가 없을 때 브라우저 위치 API로 폴백하기까지 대기 시간(ms)
const GPS_BRIDGE_TIMEOUT_MS = 5000;

type UseRunningSessionParams =
  | { mode: 'FREE' }
  | { mode: 'COURSE'; courseId: number }
  | { mode: 'GHOST'; ghostTargetRunningSessionId: number };

export function useRunningSession(params: UseRunningSessionParams) {
  const [sessionReady, setSessionReady] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [runningSessionId, setRunningSessionId] = useState<number | null>(null);

  // StrictMode 이중 마운트 및 gpsLocation 재갱신 시 중복 호출 방지
  const calledRef = useRef(false);
  // 재시도 트리거: 값이 바뀔 때마다 effect 재실행
  const [retryCount, setRetryCount] = useState(0);

  const gpsLocation = useBridgeDataStore((s) => s.gpsLocation);

  useEffect(() => {
    // 중복 호출 방어
    if (calledRef.current) return;
    let cancelled = false;

    const startSession = async (latitude: number, longitude: number) => {
      if (cancelled) return;
      calledRef.current = true;
      try {
        const res = await createRunningSession({
          userId: MOCK_USER_ID,
          startLatitude: latitude,
          startLongitude: longitude,
          // params: 의존성 배열에서 제거
          ...params,
        });
        if (!cancelled) {
          setRunningSessionId(res.data.runningSessionId);
        }
      } catch {
        if (!cancelled) setError('서버 연결에 실패했습니다. 다시 시도해주세요.');
      } finally {
        if (!cancelled) setSessionReady(true);
      }
    };

    // Bridge GPS가 이미 있으면 바로 세션 생성
    if (gpsLocation) {
      startSession(gpsLocation.latitude, gpsLocation.longitude);
      return () => { cancelled = true; };
    }

    // Bridge GPS 없으면 일정 시간 대기 후 브라우저 위치 API로 폴백
    const timeout = setTimeout(() => {
      if (calledRef.current) return;
      navigator.geolocation.getCurrentPosition(
        ({ coords }) => startSession(coords.latitude, coords.longitude),
        () => { if (!cancelled) setError('위치를 가져올 수 없습니다. 위치 권한을 확인해주세요.'); },
      );
    }, GPS_BRIDGE_TIMEOUT_MS);

    // clearTimeout: effect 재실행 시 이전 타임아웃 취소
    // clearTimeout이 없으면 브릿지 GPS로 세션을 이미 만들었는데
    // 타임아웃이 터져서 navigator.geolocation이 또 실행됨
    return () => {
      cancelled = true;
      clearTimeout(timeout);
    };
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [gpsLocation, retryCount]); // params는 매 렌더마다 새 객체이므로 의도적으로 제외

  const retry = () => {
    calledRef.current = false;
    setError(null);
    setSessionReady(false);
    setRunningSessionId(null);
    setRetryCount((c) => c + 1);
  };

  return { sessionReady, error, retry, runningSessionId };
}
