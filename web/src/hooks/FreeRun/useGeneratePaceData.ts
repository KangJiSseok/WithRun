import type { SplitResult } from '@/apis/runningSessions';

interface PaceEntry {
  km: number;
  displayPace: string;
  seconds: number;
}

export function useGeneratePaceData(splits: SplitResult[]): { paceData: PaceEntry[]; avgSeconds: number } {
  if (splits.length === 0) {
    return { paceData: [], avgSeconds: 0 };
  }

  const paceData: PaceEntry[] = splits.map((split) => {
    const secs = split.splitPaceSecPerKm;
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    return {
      km: split.splitIndex,
      displayPace: `${m}'${String(s).padStart(2, '0')}"`,
      seconds: secs,
    };
  });

  const avgSeconds = paceData.reduce((a, b) => a + b.seconds, 0) / paceData.length;

  return { paceData, avgSeconds };
}
