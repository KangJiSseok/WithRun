import { create } from 'zustand';

/**
 * 북마크 상태 오버라이드 스토어
 *
 * 서버 데이터를 시드하지 않고, 사용자가 UI에서 토글한 변경사항만 보관합니다.
 * Map의 key가 없으면 "아직 토글하지 않음" → 화면에서 서버 기본값을 사용합니다.
 *
 * @example
 * // 화면에서의 사용 (useBookmarkToggle 훅을 통해)
 * const resolved = isBookmarked(courseId, course.isBookmarked);
 * // → Map에 override가 있으면 override, 없으면 course.isBookmarked 사용
 */
type BookmarkStore = {
  /**
   * 사용자가 토글한 북마크 상태만 저장 (서버 기본값 오버라이드용)
   * - key: courseId
   * - value: 사용자가 마지막으로 토글한 isBookmarked 값
   * - undefined(key 없음): 아직 토글한 적 없음 → 서버 값이 기본값
   */
  bookmarkOverrides: Map<number, boolean>;
  /** 북마크 오버라이드를 기록한다 */
  setBookmarked: (courseId: number, isBookmarked: boolean) => void;
};

export const useBookmarkStore = create<BookmarkStore>((set) => ({
  bookmarkOverrides: new Map(),
  setBookmarked: (courseId, isBookmarked) =>
    set((state) => {
      const next = new Map(state.bookmarkOverrides);
      next.set(courseId, isBookmarked);
      return { bookmarkOverrides: next };
    }),
}));
