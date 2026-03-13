import { create } from 'zustand';

interface FreeRunResult {
  time: string;
  distance: string;
  pace: string;
  calories: number;
}

interface FreeRunResultStore {
  result: FreeRunResult | null;
  setResult: (result: FreeRunResult) => void;
  clearResult: () => void;
}

export const useFreeRunResultStore = create<FreeRunResultStore>((set) => ({
  result: null,
  setResult: (result) => set({ result }),
  clearResult: () => set({ result: null }),
}));
