import { useState } from 'react';
import { FreeRunningScreen, FreeRunResultScreen } from '@/features/FreeRun';
import { useRunningSession } from '@/shared/hooks';

export function FreeRunSessionPage() {
  const [phase, setPhase] = useState<'running' | 'result'>('running');
  const { sessionReady, error } = useRunningSession({ mode: 'FREE' });

  if (error) {
    return (
      <div className="flex h-screen flex-col items-center justify-center gap-4 bg-secondary-foreground px-6">
        <p className="body-r text-center text-[hsl(var(--text-inverse))]">{error}</p>
      </div>
    );
  }

  if (!sessionReady) {
    return (
      <div className="flex h-screen flex-col items-center justify-center gap-4 bg-secondary-foreground">
        <p className="body-r text-[hsl(var(--text-inverse))]">위치 확인 중...</p>
      </div>
    );
  }

  if (phase === 'result') return <FreeRunResultScreen />;
  return <FreeRunningScreen onFinish={() => setPhase('result')} />;
}
