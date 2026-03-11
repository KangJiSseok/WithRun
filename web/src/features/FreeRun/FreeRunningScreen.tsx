// 러닝 중 화면 (통계 3개 + 타이머 + 일시정지/종료 컨트롤)

import { useNavigate } from 'react-router-dom';
import { Pause, Play, Square } from 'lucide-react';
import { cn } from '@/shared/utils';
import { useFreeRunning } from '@/hooks/FreeRun/useFreeRunning';

// startCard 컴포넌트에 props 전달 시 TypeScript 오류 방지 위해 별도 컴포넌트로 분리
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
  const { time, distance, pace, calories, isPaused, togglePause } = useFreeRunning();

  return (
    <div className="absolute inset-0 bg-secondary-foreground flex flex-col items-center justify-between px-6 py-12">
      {/* 상단 통계 */}
      <div className="w-full flex gap-3 mt-4">
        <StatCard label="거리" value={distance} unit="km" />
        <StatCard label="페이스" value={pace} unit="/km" />
        {/* StartCard의 value prop이 string 타입이어야 함 */}
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
          {/* 버튼 상태 뜨는거 */}
          {isPaused ? '일시정지' : '러닝 중'}
        </span>
      </div>

      {/* 하단 컨트롤 */}
      <div className="relative flex items-center justify-center mb-4 w-full">
        {/* 일시정지/재개 버튼 — 항상 가운데 고정 */}
        <button
          onClick={togglePause}
          className="w-20 h-20 rounded-full bg-primary flex items-center justify-center text-[hsl(var(--text-inverse))] shadow-[0_0_30px_hsl(var(--primary)/50%)] z-10"
          // aria-label은 접근성 향상을 위해 버튼의 기능을 명확히 설명하는 텍스트로 설정
          // (시각 장애인용 보조 기술)을 위한 접근성 속성
          aria-label={isPaused ? '러닝 재개' : '일시정지'}
        >
          {/* 크기도 고정 */}
          {isPaused ? (
            <Play size={30} fill="currentColor" />
          ) : (
            <Pause size={30} fill="currentColor" />
          )}
        </button>

        {/* 종료 버튼 — 일시정지 시 오른쪽으로 슬라이드 */}
        <button
          onClick={() => navigate('/freerun-result')}
          className={cn(
            'absolute w-14 h-14 rounded-full bg-[hsl(var(--secondary-3))] flex items-center justify-center text-[hsl(var(--text-inverse))] transition-all duration-300',
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
