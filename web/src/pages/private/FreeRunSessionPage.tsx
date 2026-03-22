import { useEffect, useState } from 'react';
import { FreeRunningScreen, FreeRunResultScreen } from '@/features/FreeRun';
import { useRunningSession } from '@/shared/hooks';
import { useRunningSessionStore } from '@/stores/runningSession';

export function FreeRunSessionPage() {
  const [phase, setPhase] = useState<'running' | 'result'>('running');
  const clear = useRunningSessionStore((s) => s.clear);
  const { sessionReady, error, retry } = useRunningSession({ mode: 'FREE' });

  useEffect(() => {
    clear();
  }, []);

  if (error) {
    return (
      <div className="flex h-screen flex-col items-center justify-center gap-4 bg-secondary-foreground px-6">
        <p className="body-r text-center text-[hsl(var(--text-inverse))]">{error}</p>
        <button onClick={retry} className="body-r rounded-lg bg-primary px-6 py-3 text-white">
          다시 시도
        </button>
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
