import { useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';

import { addBookmarkApi, deleteBookmarkApi } from '@/apis/courseBookmark';
import { useBookmarkStore } from '@/stores/Bookmark/useBookmarkStore';

/**
 * 북마크 토글 훅
 *
 * 오버라이드 패턴으로 동작:
 * - 서버 데이터(course.isBookmarked)를 기본값으로 사용
 * - 사용자가 토글하면 Zustand에 오버라이드를 기록
 * - 화면에서는 isBookmarked(courseId, serverDefault)로 최종 상태를 resolve
 *
 * @example
 * const { isBookmarked, toggle } = useBookmarkToggle();
 *
 * // 화면에서 북마크 상태 결정 (오버라이드 > 서버 기본값)
 * const saved = isBookmarked(course.courseId, course.isBookmarked);
 *
 * // 북마크 토글 (서버 기본값을 함께 전달)
 * toggle(course.courseId, course.isBookmarked);
 */
export function useBookmarkToggle() {
  const queryClient = useQueryClient();
  const { bookmarkOverrides, setBookmarked } = useBookmarkStore();

  /**
   * 현재 북마크 상태를 resolve한다.
   * Zustand 오버라이드가 있으면 그 값을, 없으면 서버 기본값을 반환.
   *
   * @param courseId - 코스 ID
   * @param serverDefault - 서버 API 응답의 isBookmarked 값 (course.isBookmarked)
   */
  function isBookmarked(courseId: number, serverDefault: boolean): boolean {
    const override = bookmarkOverrides.get(courseId);
    return override !== undefined ? override : serverDefault;
  }

  /**
   * 북마크를 토글한다.
   *
   * 1) 낙관적 업데이트: isBookmarked 반전 → UI 즉시 반영
   * 2) API 호출: add / delete 요청
   * 3) 성공 시: 관련 쿼리 캐시 stale 마킹
   * 4) 실패 시: 원래 상태로 롤백
   *
   * @param courseId - 코스 ID
   * @param serverIsBookmarked - 서버 API 응답의 isBookmarked (resolve 기본값)
   */
  async function toggle(courseId: number, serverIsBookmarked: boolean) {
    const currentIsSaved = isBookmarked(courseId, serverIsBookmarked);

    // 1. 낙관적 업데이트
    setBookmarked(courseId, !currentIsSaved);

    try {
      // 2. API 호출
      if (currentIsSaved) {
        await deleteBookmarkApi(courseId);
        toast.success('저장이 해제되었어요.');
      } else {
        await addBookmarkApi(courseId);
        toast.success('코스가 저장되었어요.');
      }

      // 3. 관련 쿼리 캐시 처리
      //    - bookmarkedCourses: 즉시 refetch (북마크 목록 갱신)
      //    - nearbyCourses / nearbyGhostCourses: stale 마킹만
      //      → 현재 페이지에서는 refetch 안 하고, 다음 진입 시 자동 refetch
      void queryClient.invalidateQueries({ queryKey: ['bookmarkedCourses'] });
      void queryClient.invalidateQueries({ queryKey: ['nearbyCourses'], refetchType: 'none' });
      void queryClient.invalidateQueries({ queryKey: ['nearbyGhostCourses'], refetchType: 'none' });
    } catch {
      // 4. 롤백
      setBookmarked(courseId, currentIsSaved);
      toast.error('다시 시도해주세요.');
    }
  }

  return { isBookmarked, toggle };
}
