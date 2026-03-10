import type { RunCharacterProps } from './RunCharacter.types';

export function RunCharacter({ isRunning, onClick }: RunCharacterProps) {
  return (
    <button
      onClick={onClick}
      aria-label={isRunning ? '러닝 종료' : '러닝 시작'}
      className="flex flex-col items-center gap-2 focus:outline-none"
    >
      {/* 캐릭터 아바타 */}
      <div
        className={[
          'relative w-24 h-24 rounded-full flex items-center justify-center text-5xl select-none shadow-xl',
          isRunning ? 'animate-bounce' : 'animate-pulse',
        ].join(' ')}
        style={{
          backgroundColor: isRunning
            ? 'hsl(var(--primary-1))'
            : 'hsl(var(--secondary-1))',
          transition: 'background-color 0.3s ease',
        }}
      >
        {isRunning ? '🏃' : '🧍'}

        {/* 러닝 중 파동 효과 */}
        {isRunning && (
          <span
            className="absolute inset-0 rounded-full animate-ping opacity-30"
            style={{ backgroundColor: 'hsl(var(--primary-3))' }}
          />
        )}
      </div>

      {/* 상태 레이블 */}
      <span
        className="caption-b px-4 py-1 rounded-full"
        style={{
          backgroundColor: isRunning
            ? 'hsl(var(--primary-1))'
            : 'hsl(var(--secondary-1) / 85%)',
          color: 'hsl(var(--text-inverse))',
        }}
      >
        {isRunning ? '탭하여 종료' : '탭하여 시작'}
      </span>
    </button>
  );
}
