import { Pause, Play, Square } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

import { FreeRunMap } from '@/features/FreeRun/FreeRunMap';
import { useFreeRunning } from '@/hooks/FreeRun/useFreeRunning';
import { cn } from '@/shared/utils';

interface StatItemProps {
  label: string;
  value: string;
  valueClassName?: string;
  sub?: React.ReactNode;
}

function StatItem({ label, value, valueClassName, sub }: StatItemProps) {
  return (
    <div className="flex flex-col items-center gap-1">
      <span className="body-r tracking-wide text-gray-600">{label}</span>
      <span
        className={cn(
          'h2-b tabular-nums [text-shadow:0_2px_8px_rgba(0,0,0,0.9)]',
          valueClassName ?? 'text-white'
        )}
      >
        {value}
      </span>
      {sub && <span className="body-r font-semibold text-gray-600">{sub}</span>}
    </div>
  );
}

export function NormalRunningScreen() {
  const navigate = useNavigate();
  const { time, distance, pace, calories, isPaused, togglePause } =
    useFreeRunning();

  return (
    <div className="absolute inset-0 overflow-hidden">
      {/* 지도 */}
      <FreeRunMap />

      {/* 상단 통계 오버레이 */}
      <div className="absolute left-0 right-0 top-0 z-sticky flex justify-between px-8 pt-14">
        <StatItem label="거리" value={distance} sub="km" />
        <StatItem label="페이스" value={pace} />
        <StatItem
          label="칼로리"
          value={String(calories)}
          valueClassName="text-primary"
          sub={<span className="text-primary">🔥</span>}
        />
      </div>

      {/* 하단 그라데이션 + 컨트롤 */}
      <div className="absolute bottom-0 left-0 right-0 z-sticky">
        <div className="h-52 bg-gradient-to-b from-transparent to-black/90" />

        <div className="flex flex-col items-center gap-5 bg-black/90 pb-14 pt-2">
          {/* 타이머 */}
          <div className="flex flex-col items-center gap-2">
            <span
              className={cn(
                'font-bold leading-none tabular-nums text-white',
                time.length > 5 ? 'text-7xl' : 'text-8xl'
              )}
            >
              {time}
            </span>
            <span className="caption-r tracking-[0.2em] text-white/50 uppercase">
              Total Time
            </span>
          </div>

          {/* 버튼 영역 */}
          <div className="relative flex w-full items-center justify-center">
            {/* 일시정지 / 재개 */}
            <button
              onClick={togglePause}
              className="z-10 flex h-20 w-20 items-center justify-center rounded-full bg-primary text-white shadow-[0_0_30px_hsl(var(--primary)/50%)]"
              aria-label={isPaused ? '러닝 재개' : '일시정지'}
            >
              {isPaused ? (
                <Play size={30} fill="currentColor" />
              ) : (
                <Pause size={30} fill="currentColor" />
              )}
            </button>

            {/* 종료 버튼 (일시정지 시 등장) */}
            <button
              onClick={() => navigate('/normal')}
              className={cn(
                'absolute flex h-14 w-14 items-center justify-center rounded-full bg-white/20 text-white transition-all duration-300',
                isPaused
                  ? 'translate-x-[80px] opacity-100'
                  : 'pointer-events-none translate-x-[50px] opacity-0'
              )}
              aria-label="러닝 종료"
            >
              <Square size={22} fill="currentColor" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
