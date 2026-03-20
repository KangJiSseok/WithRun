import { create } from 'zustand';

import type { RunningMode } from '@/shared/types/api.type';

interface RunningResult {
  durationSec: number;
  distanceM: number;
  caloriesKcal: number;
  avgPaceSecPerKm: number;
}

interface RunningSessionStore {
  runningSessionId: number | null;
  courseId: number | null;
  runningMode: RunningMode | null;
  result: RunningResult | null;
  setRunningSessionId: (id: number) => void;
  setCourseId: (id: number) => void;
  setRunningMode: (mode: RunningMode) => void;
  setResult: (result: RunningResult) => void;
  clear: () => void;
}

export const useRunningSessionStore = create<RunningSessionStore>((set) => ({
  runningSessionId: null,
  courseId: null,
  runningMode: null,
  result: null,
  setRunningSessionId: (id) => set({ runningSessionId: id }),
  setCourseId: (id) => set({ courseId: id }),
  setRunningMode: (mode) => set({ runningMode: mode }),
  setResult: (result) => set({ result }),
  clear: () => set({ runningSessionId: null, courseId: null, runningMode: null, result: null }),
}));
