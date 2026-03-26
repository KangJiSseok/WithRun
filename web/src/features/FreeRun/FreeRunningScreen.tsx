// 러닝 중 화면 (통계 3개 + 타이머 + 일시정지/종료 컨트롤)

import { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { Pause, Play, Square } from 'lucide-react';
import { cn } from '@/shared/utils';
import { useFreeRunning } from '@/hooks/FreeRun/useFreeRunning';
import { useBridgeDataStore } from '@/bridge';
import { completeRunningSession } from '@/apis';
import { useFreeRunSessionStore } from '@/stores/FreeRun';

// 회원 시스템 구현 전 임시 mock 유저
const MOCK_USER_ID = 1;

interface StatCardProps {
  label: string;
  value: string;
  unit: string;
}

function StatCard({ label, value, unit }: StatCardProps) {
  return (
    <div className="flex-1 flex flex-col items-center gap-1 bg-[hsl(var(--secondary-2))] rounded-2xl py-5 px-2">
      <span className="caption-r text-[hsl(var(--secondary-5))]">{label}</span>
      <span className="h3-b text-[hsl(var(--text-inverse))]">{value}</span>
      <span className="caption-r text-[hsl(var(--secondary-5))]">{unit}</span>
    </div>
  );
}

export function FreeRunningScreen() {
  const navigate = useNavigate();
  const { time, distance, pace, calories, isPaused, togglePause, getCollectedData } =
    useFreeRunning();
  const gpsLocation = useBridgeDataStore((s) => s.gpsLocation);
  const session = useFreeRunSessionStore((s) => s.session);
  const setResult = useFreeRunSessionStore((s) => s.setResult);
  const clearAll = useFreeRunSessionStore((s) => s.clearAll);

  const [isEnding, setIsEnding] = useState(false);

  // "latest ref" 패턴: cleanup 클로저에서 최신값 접근
  const latestGetCollectedDataRef = useRef(getCollectedData);
  latestGetCollectedDataRef.current = getCollectedData;

  const latestGpsLocationRef = useRef(gpsLocation);
  latestGpsLocationRef.current = gpsLocation;

  // 비정상 종료 처리 (앱 이탈 시 세션 종료 API fire-and-forget)
  // DEV: StrictMode 이중 마운트로 인한 오발을 완전히 차단하기 위해 skip
  //      (비정상 종료 처리는 프로덕션 모바일에서만 필요)
  // handleStopRunning 에러 시 clearAll() → session=null → !s 체크에서 자동 방지
  useEffect(() => {
    if (import.meta.env.DEV) return;
    return () => {
      const { session: s, result: r, clearAll: ca } = useFreeRunSessionStore.getState();
      if (!s || r) return;
      const data = latestGetCollectedDataRef.current();
      const gps = latestGpsLocationRef.current;
      completeRunningSession(s.runningSessionId, {
        userId: MOCK_USER_ID,
        ...data,
        endLatitude: gps?.latitude ?? 0,
        endLongitude: gps?.longitude ?? 0,
        distanceGapM: 0,
      }).catch(() => {}); // fire-and-forget
      ca();
    };
  }, []);

  const handleStopRunning = useCallback(async () => {
    if (isEnding || !session) return;
    setIsEnding(true);
    try {
      const data = getCollectedData();
      const res = await completeRunningSession(session.runningSessionId, {
        userId: MOCK_USER_ID,
        ...data,
        endLatitude: gpsLocation?.latitude ?? 0,
        endLongitude: gpsLocation?.longitude ?? 0,
        distanceGapM: 0,
      });
      setResult(res.data);
    } catch {
      clearAll();
      navigate('/free-run');
    } finally {
      setIsEnding(false);
    }
  }, [isEnding, session, getCollectedData, gpsLocation, setResult, clearAll, navigate]);

  return (
    <div className="absolute inset-0 bg-secondary-foreground flex flex-col items-center justify-between px-6 py-12">
      {/* 상단 통계 */}
      <div className="w-full flex gap-3 mt-4">
        <StatCard label="거리" value={distance} unit="km" />
        <StatCard label="페이스" value={pace} unit="/km" />
        <StatCard label="칼로리" value={String(calories)} unit="kcal" />
      </div>

      {/* 중앙 타이머 */}
      <div className="flex flex-col items-center gap-3">
        <span className="caption-m text-[hsl(var(--secondary-5))] tracking-widest uppercase">
          러닝 시간
        </span>
        <span
          className={cn(
            'font-bold leading-none text-[hsl(var(--text-inverse))] tabular-nums',
            time.length > 5 ? 'text-7xl' : 'text-8xl',
          )}
        >
          {time}
        </span>
        <span
          className={cn(
            'caption-b px-3 py-1 rounded-full',
            isPaused
              ? 'bg-[hsl(var(--secondary-4))] text-[hsl(var(--secondary-5))]'
              : 'bg-primary/20 text-primary',
          )}
        >
          {isPaused ? '일시정지' : '러닝 중'}
        </span>
      </div>

      {/* 하단 컨트롤 */}
      <div className="relative flex items-center justify-center mb-4 w-full">
        {/* 일시정지/재개 버튼 */}
        <button
          onClick={togglePause}
          disabled={isEnding}
          className="w-20 h-20 rounded-full bg-primary flex items-center justify-center text-[hsl(var(--text-inverse))] shadow-[0_0_30px_hsl(var(--primary)/50%)] z-10 disabled:opacity-50"
          aria-label={isPaused ? '러닝 재개' : '일시정지'}
        >
          {isPaused ? (
            <Play size={30} fill="currentColor" />
          ) : (
            <Pause size={30} fill="currentColor" />
          )}
        </button>

        {/* 종료 버튼 — 일시정지 시 오른쪽으로 슬라이드 */}
        <button
          onClick={handleStopRunning}
          disabled={isEnding}
          className={cn(
            'absolute w-14 h-14 rounded-full bg-[hsl(var(--secondary-3))] flex items-center justify-center text-[hsl(var(--text-inverse))] transition-all duration-300 disabled:opacity-50',
            isPaused
              ? 'opacity-100 translate-x-[80px]'
              : 'opacity-0 translate-x-[50px] pointer-events-none',
          )}
          aria-label={isEnding ? '종료 중...' : '러닝 종료'}
        >
          <Square size={22} fill="currentColor" />
        </button>
      </div>
    </div>
  );
}
