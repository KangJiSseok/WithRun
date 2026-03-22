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
  mode: RunningMode | null;
  result: RunningResult | null;
  setRunningSessionId: (id: number) => void;
  setCourseId: (id: number) => void;
  setMode: (mode: RunningMode) => void;
  setResult: (result: RunningResult) => void;
  clear: () => void;
}

export const useRunningSessionStore = create<RunningSessionStore>((set) => ({
  runningSessionId: null,
  courseId: null,
  mode: null,
  result: null,
  setRunningSessionId: (id) => set({ runningSessionId: id }),
  setCourseId: (id) => set({ courseId: id }),
  setMode: (mode) => set({ mode }),
  setResult: (result) => set({ result }),
  clear: () => set({ runningSessionId: null, courseId: null, mode: null, result: null }),
}));
