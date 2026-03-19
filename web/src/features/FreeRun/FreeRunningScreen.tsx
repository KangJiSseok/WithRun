// 러닝 중 화면 (통계 3개 + 타이머 + 일시정지/종료 컨트롤)

import { useState } from 'react';
import { Pause, Play, Square } from 'lucide-react';
import { cn } from '@/shared/utils';
import { useFreeRunning } from '@/hooks/FreeRun/useFreeRunning';
import { useRunningSessionStore } from '@/stores/runningSession';
import { useBridgeDataStore } from '@/bridge';
import { completeRunningSession } from '@/apis';

// 회원 시스템 구현 전 임시 mock 유저
const MOCK_USER_ID = 1;

function parseDurationSec(formatted: string): number {
  const parts = formatted.split(':').map(Number);
  if (parts.length === 3) return parts[0] * 3600 + parts[1] * 60 + parts[2];
  return parts[0] * 60 + parts[1];
}

interface FreeRunningScreenProps {
  onFinish: () => void;
}

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

export function FreeRunningScreen({ onFinish }: FreeRunningScreenProps) {
  const { time, distance, pace, calories, isPaused, togglePause } = useFreeRunning();
  const gpsLocation = useBridgeDataStore((s) => s.gpsLocation);
  const setResult = useRunningSessionStore((s) => s.setResult);
  const setRunningMode = useRunningSessionStore((s) => s.setRunningMode);
  const runningSessionId = useRunningSessionStore((s) => s.runningSessionId);

  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [localResult, setLocalResult] = useState<{
    durationSec: number;
    distanceM: number;
    caloriesKcal: number;
    avgPaceSecPerKm: number;
  } | null>(null);

  const buildPayload = () => {
    const durationSec = parseDurationSec(time);
    const distanceM = Math.round(parseFloat(distance) * 1000);
    const avgSpeedMps = durationSec > 0 ? distanceM / durationSec : 0;
    // TODO: 웹 테스트용 fallback — 실제 이동 데이터 없을 때 300(5분/km) 임시 사용
    const avgPaceSecPerKm = avgSpeedMps > 0 ? Math.round(1000 / avgSpeedMps) : 300;
    return { durationSec, distanceM, avgSpeedMps, avgPaceSecPerKm };
  };

  const handleStop = async () => {
    const { durationSec, distanceM, avgSpeedMps, avgPaceSecPerKm } = buildPayload();
    setLocalResult({ durationSec, distanceM, caloriesKcal: calories, avgPaceSecPerKm });
    setSubmitError(null);
    setIsSubmitting(true);
    setRunningMode('FREE');

    try {
      const res = await completeRunningSession(runningSessionId!, {
        userId: MOCK_USER_ID,
        distanceM,
        caloriesKcal: calories,
        endLatitude: gpsLocation?.latitude ?? 0,
        endLongitude: gpsLocation?.longitude ?? 0,
        avgSpeedMps,
        durationSec,
        avgPaceSecPerKm,
        elevationGainM: 0,
        distanceGapM: 0,
        gpsSamples: [],
        healthSamples: [],
        splits: [],
      });
      setResult({
        durationSec: res.data.durationSec,
        distanceM: res.data.distanceM,
        caloriesKcal: res.data.caloriesKcal,
        avgPaceSecPerKm: res.data.avgPaceSecPerKm,
      });
      onFinish();
    } catch {
      setSubmitError('서버 연결에 실패했습니다.');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleForceFinish = () => {
    if (!localResult) return;
    setResult(localResult);
    onFinish();
  };

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

      {/* 에러 메시지 + 재시도/결과보기 버튼 */}
      {submitError && (
        <div className="w-full flex flex-col items-center gap-3">
          <span className="caption-r text-red-400">{submitError}</span>
          <div className="flex gap-3 w-full">
            <button
              onClick={handleStop}
              className="flex-1 py-3 rounded-2xl bg-primary text-[hsl(var(--text-inverse))] caption-b"
            >
              재시도
            </button>
            <button
              onClick={handleForceFinish}
              className="flex-1 py-3 rounded-2xl bg-[hsl(var(--secondary-3))] text-[hsl(var(--text-inverse))] caption-b"
            >
              결과 보기
            </button>
          </div>
        </div>
      )}

      {/* 하단 컨트롤 */}
      <div className="relative flex items-center justify-center mb-4 w-full">
        {/* 일시정지/재개 버튼 */}
        <button
          onClick={togglePause}
          disabled={isSubmitting}
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
          onClick={handleStop}
          disabled={isSubmitting}
          className={cn(
            'absolute w-14 h-14 rounded-full bg-[hsl(var(--secondary-3))] flex items-center justify-center text-[hsl(var(--text-inverse))] transition-all duration-300 disabled:opacity-50',
            isPaused
              ? 'opacity-100 translate-x-[80px]'
              : 'opacity-0 translate-x-[50px] pointer-events-none',
          )}
          aria-label="러닝 종료"
        >
          <Square size={22} fill="currentColor" />
        </button>
      </div>
    </div>
  );
}
