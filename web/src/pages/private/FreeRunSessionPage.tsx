import { FreeRunningScreen, FreeRunResultScreen } from '@/features/FreeRun';
import { useFreeRunSessionStore } from '@/stores/FreeRun';

export function FreeRunSessionPage() {
  const result = useFreeRunSessionStore((s) => s.result);

  if (result) {
    return <FreeRunResultScreen result={result} />;
  }

  return <FreeRunningScreen />;
}
