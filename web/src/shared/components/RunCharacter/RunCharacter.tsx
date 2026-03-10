import { Mascot } from '@/shared/components/Mascot';
import type { RunCharacterProps } from './RunCharacter.types';

export function RunCharacter({ isRunning, onClick }: RunCharacterProps) {
  return (
    <button
      onClick={onClick}
      aria-label={isRunning ? '러닝 종료' : '러닝 시작'}
      className="flex flex-col items-center gap-4 focus:outline-none hover:scale-105 active:scale-95 transition-transform"
    >
      {/* 캐릭터 원형 프레임 */}
      <div className="relative">
        {/* 외부 glow 효과 */}
        <div
          className="absolute inset-0 rounded-full blur-2xl animate-pulse"
          style={{ backgroundColor: 'hsl(var(--primary-1) / 30%)' }}
        />
        {/* 원형 버튼 */}
        <div
          className="w-32 h-32 backdrop-blur-md rounded-full border-4 flex items-center justify-center relative overflow-hidden"
          style={{
            backgroundColor: 'hsl(var(--secondary-1) / 60%)',
            borderColor: 'hsl(var(--primary-1))',
            boxShadow: '0 0 40px hsl(var(--primary-1) / 60%)',
          }}
        >
          <div className="scale-[1.5] translate-y-2">
            <Mascot />
          </div>
        </div>
      </div>

      {/* 상태 레이블 */}
      <span
        className="caption-b px-4 py-1 rounded-full font-bold tracking-widest animate-bounce"
        style={{
          color: 'hsl(var(--text-inverse))',
          textShadow: '0 2px 4px hsl(var(--black) / 80%)',
        }}
      >
        {isRunning ? '탭하여 종료' : 'RUN!'}
      </span>
    </button>
  );
}
