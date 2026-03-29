import { create } from 'zustand';

/**
 * 좋아요 상태 오버라이드 스토어
 *
 * 서버 데이터를 시드하지 않고, 사용자가 UI에서 토글한 변경사항만 보관합니다.
 * Map의 key가 없으면 "아직 토글하지 않음" → 화면에서 서버 기본값을 사용합니다.
 *
 * @example
 * // 화면에서의 사용 (useLikeToggle 훅을 통해)
 * const resolved = isLiked(courseId, course.isLiked);
 * // → Map에 override가 있으면 override, 없으면 course.isLiked 사용
 */
type LikeStore = {
  /**
   * 사용자가 토글한 좋아요 상태만 저장 (서버 기본값 오버라이드용)
   * - key: courseId
   * - value: 사용자가 마지막으로 토글한 isLiked 값
   * - undefined(key 없음): 아직 토글한 적 없음 → 서버 값이 기본값
   */
  likeOverrides: Map<number, boolean>;
  /** 서버 응답의 likeCount를 캐싱. UI에서 즉각 증감 합산용 */
  likeCounts: Record<number, number>;
  /** 좋아요 오버라이드를 기록한다 */
  setLiked: (courseId: number, isLiked: boolean) => void;
  /** 좋아요 수를 갱신한다 (낙관적 업데이트 + API 응답 반영) */
  setCount: (courseId: number, count: number) => void;
};

export const useLikeStore = create<LikeStore>((set) => ({
  likeOverrides: new Map(),
  likeCounts: {},
  setLiked: (courseId, isLiked) =>
    set((state) => {
      const next = new Map(state.likeOverrides);
      next.set(courseId, isLiked);
      return { likeOverrides: next };
    }),
  setCount: (courseId, count) =>
    set((state) => ({
      likeCounts: { ...state.likeCounts, [courseId]: count },
    })),
}));
