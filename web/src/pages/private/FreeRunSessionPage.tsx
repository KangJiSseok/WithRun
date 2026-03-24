import { useState } from 'react';
import { FreeRunningScreen, FreeRunResultScreen } from '@/features/FreeRun';
import { useRunningSession } from '@/shared/hooks';

interface RunningResult {
  durationSec: number;
  distanceM: number;
  caloriesKcal: number;
  avgPaceSecPerKm: number;
}

export function FreeRunSessionPage() {
  const [phase, setPhase] = useState<'running' | 'result'>('running');
  const [result, setResult] = useState<RunningResult | null>(null);
  const [courseId, setCourseId] = useState<number | null>(null);

  const { sessionReady, error, retry, runningSessionId } = useRunningSession({ mode: 'FREE' });

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

  if (phase === 'result') {
    return (
      <FreeRunResultScreen
        result={result}
        courseId={courseId}
        runningSessionId={runningSessionId}
        onCourseRegistered={setCourseId}
      />
    );
  }

  return (
    <FreeRunningScreen
      runningSessionId={runningSessionId}
      onFinish={(r) => {
        setResult(r);
        setPhase('result');
      }}
    />
  );
}
