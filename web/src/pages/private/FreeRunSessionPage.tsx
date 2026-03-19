import { useState } from 'react';
import { FreeRunningScreen, FreeRunResultScreen } from '@/features/FreeRun';

export function FreeRunSessionPage() {
  const [phase, setPhase] = useState<'running' | 'result'>('running');

  if (phase === 'result') return <FreeRunResultScreen />;
  return <FreeRunningScreen onFinish={() => setPhase('result')} />;
}
