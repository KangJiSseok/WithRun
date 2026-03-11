import { useState } from 'react';

export function useFreeRun() {
  const [isRunning, setIsRunning] = useState(false);

  const startRun = () => setIsRunning(true);
  const stopRun = () => setIsRunning(false);

  return { isRunning, startRun, stopRun };
}
