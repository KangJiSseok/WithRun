import { useState } from 'react';
import { useLocation } from 'react-router-dom';
import { FreeRunningScreen, FreeRunResultScreen } from '@/features/FreeRun';
import type { RunningResult } from '@/features/FreeRun/FreeRunningScreen';

export function FreeRunSessionPage() {
  const { state } = useLocation();
  const runningSessionId: number | null = state?.runningSessionId ?? null;

  const [phase, setPhase] = useState<'running' | 'result'>('running');
  const [result, setResult] = useState<RunningResult | null>(null);
  const [courseId, setCourseId] = useState<number | null>(null);

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
