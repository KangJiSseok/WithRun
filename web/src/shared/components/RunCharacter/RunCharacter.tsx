import { Mascot } from '@/shared/components/Mascot';
import { cn } from '@/shared/utils';
import type { RunCharacterProps } from './RunCharacter.types';

export function RunCharacter({ onClick, disabled }: RunCharacterProps) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      aria-label="러닝 시작"
      className={cn('flex flex-col items-center gap-4 focus:outline-none transition-transform', !disabled && 'hover:scale-105 active:scale-95')}
    >
      {/* 캐릭터 원형 프레임 */}
      <div className="relative">
        {/* 외부 glow 효과 */}
        <div className="absolute inset-0 rounded-full blur-2xl animate-pulse bg-primary/30" />
        {/* 원형 버튼 */}
        <div
          className="w-32 h-32 backdrop-blur-md rounded-full border-4 border-primary flex items-center justify-center relative overflow-hidden bg-secondary-foreground/60 shadow-[0_0_40px_hsl(var(--primary)/60%)]"
        >
          <div className="scale-[1.5] translate-y-2">
            <Mascot />
          </div>
        </div>
      </div>

      {/* 상태 레이블 */}
      <span
        className="caption-b px-4 py-1 rounded-full font-bold tracking-widest animate-bounce text-[hsl(var(--text-inverse))] text-shadow-[0_2px_4px_hsl(var(--black)/80%)]"
      >
        {disabled ? '로딩중..' : 'RUN!'}
      </span>
    </button>
  );
}
