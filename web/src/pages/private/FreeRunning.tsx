import { useState } from 'react';
import { FreeRunningScreen, FreeRunResultScreen } from '@/features/FreeRun';

export function FreeRunningPage() {
  const [phase, setPhase] = useState<'running' | 'result'>('running');

  if (phase === 'result') return <FreeRunResultScreen />;
  return <FreeRunningScreen onFinish={() => setPhase('result')} />;
}
