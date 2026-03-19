import { create } from 'zustand';

interface FreeRunResult {
  durationSec: number;
  distanceM: number;
  caloriesKcal: number;
  avgPaceSecPerKm: number;
}

interface FreeRunResultStore {
  result: FreeRunResult | null;
  // 러닝 세션 ID - 러닝 종료 시 POST /api/runs 응답으로 저장
  // 추후 API 연동 시: FreeRunningScreen 종료 버튼 onClick에서 setRunningSessionId(res.data.data.runningSessionId) 호출
  runningSessionId: number | null;
  // 커뮤니티 코스 등록 후 반환되는 courseId - 좋아요 버튼 API에 사용
  courseId: number | null;
  setResult: (result: FreeRunResult) => void;
  setRunningSessionId: (runningSessionId: number) => void;
  setCourseId: (courseId: number) => void;
  clearResult: () => void;
}

export const useFreeRunResultStore = create<FreeRunResultStore>((set) => ({
  result: null,
  runningSessionId: null,
  courseId: null,
  setResult: (result) => set({ result }),
  setRunningSessionId: (runningSessionId) => set({ runningSessionId }),
  setCourseId: (courseId) => set({ courseId }),
  clearResult: () => set({ result: null, runningSessionId: null, courseId: null }),
}));
