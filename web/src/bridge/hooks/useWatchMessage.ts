import { useBridgeDataStore } from '../stores/useBridgeDataStore';

export function useWatchMessage() {
  const watchData = useBridgeDataStore((s) => s.watchHealthData);
  return { watchData };
}
