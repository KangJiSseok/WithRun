import { create } from 'zustand';

interface FreeRunResult {
  time: string;
  distance: string;
  pace: string;
  calories: number;
}

interface FreeRunResultStore {
  result: FreeRunResult | null;
  // 러닝 종료 시 POST /api/runs 응답으로 자동 생성되는 courseId
  // 추후 API 연동 시: FreeRunningScreen 종료 버튼 onClick에서 setCourseId(res.data.data.courseId) 호출
  courseId: number | null;
  setResult: (result: FreeRunResult) => void;
  setCourseId: (courseId: number) => void;
  clearResult: () => void;
}

export const useFreeRunResultStore = create<FreeRunResultStore>((set) => ({
  result: null,
  courseId: null,
  setResult: (result) => set({ result }),
  setCourseId: (courseId) => set({ courseId }),
  clearResult: () => set({ result: null, courseId: null }),
}));
