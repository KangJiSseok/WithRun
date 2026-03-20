import { create } from 'zustand';

import type { RunningSessionData } from '@/apis/runningSessions';

interface NormalRunSessionStore {
  // createRunningSession API 응답 데이터 (세션 시작 시 저장, 종료 시 초기화)
  session: RunningSessionData | null;
  setSession: (session: RunningSessionData) => void;
  clearSession: () => void;
}

export const useNormalRunSessionStore = create<NormalRunSessionStore>((set) => ({
  session: null,
  setSession: (session) => set({ session }),
  clearSession: () => set({ session: null }),
}));
