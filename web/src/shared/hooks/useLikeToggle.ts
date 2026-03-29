import { useQueryClient } from '@tanstack/react-query';
import { toast } from 'sonner';

import { likeCourse, unlikeCourse } from '@/apis/courseLike';
import { useLikeStore } from '@/stores/Like/useLikeStore';

/**
 * 좋아요 토글 훅
 *
 * 오버라이드 패턴으로 동작:
 * - 서버 데이터(course.isLiked)를 기본값으로 사용
 * - 사용자가 토글하면 Zustand에 오버라이드를 기록
 * - 화면에서는 isLiked(courseId, serverDefault)로 최종 상태를 resolve
 *
 * @example
 * const { isLiked, getLikeCount, toggle } = useLikeToggle();
 *
 * // 화면에서 좋아요 상태 결정 (오버라이드 > 서버 기본값)
 * const liked = isLiked(course.courseId, course.isLiked);
 *
 * // 좋아요 토글 (서버 기본값을 함께 전달)
 * toggle(course.courseId, course.isLiked, course.likeCount);
 */
export function useLikeToggle() {
  const queryClient = useQueryClient();
  const { likeOverrides, likeCounts, setLiked, setCount } = useLikeStore();

  /**
   * 현재 좋아요 상태를 resolve한다.
   * Zustand 오버라이드가 있으면 그 값을, 없으면 서버 기본값을 반환.
   *
   * @param courseId - 코스 ID
   * @param serverDefault - 서버 API 응답의 isLiked 값 (course.isLiked)
   */
  function isLiked(courseId: number, serverDefault: boolean): boolean {
    const override = likeOverrides.get(courseId);
    return override !== undefined ? override : serverDefault;
  }

  /**
   * 좋아요 수를 resolve한다.
   * 낙관적 업데이트로 캐싱된 값이 있으면 그 값을, 없으면 서버 기본값을 반환.
   */
  function getLikeCount(courseId: number, fallback: number) {
    return likeCounts[courseId] ?? fallback;
  }

  /**
   * 좋아요를 토글한다.
   *
   * 1) 낙관적 업데이트: isLiked 반전 + likeCount 증감 → UI 즉시 반영
   * 2) API 호출: like / unlike 요청
   * 3) 성공 시: 서버 likeCount로 보정 + 관련 쿼리 캐시 stale 마킹
   * 4) 실패 시: 원래 상태로 롤백
   *
   * @param courseId - 코스 ID
   * @param serverIsLiked - 서버 API 응답의 isLiked (resolve 기본값)
   * @param fallbackCount - 서버 API 응답의 likeCount (likeCounts에 없을 때 사용)
   */
  async function toggle(courseId: number, serverIsLiked: boolean, fallbackCount: number) {
    const currentIsLiked = isLiked(courseId, serverIsLiked);
    const currentCount = likeCounts[courseId] ?? fallbackCount;

    // 1. 낙관적 업데이트 — UI에 즉시 반영
    setLiked(courseId, !currentIsLiked);
    setCount(courseId, currentIsLiked ? currentCount - 1 : currentCount + 1);

    try {
      // 2. API 호출
      if (currentIsLiked) {
        const res = await unlikeCourse(courseId);
        setCount(courseId, res.data.likeCount);
        toast.success('좋아요를 취소했어요.');
      } else {
        const res = await likeCourse(courseId);
        setCount(courseId, res.data.likeCount);
        toast.success('좋아요를 눌렀어요.');
      }

      // 3. 관련 쿼리 캐시 처리
      //    - bookmarkedCourses: 즉시 refetch (북마크 목록에서 좋아요 수 갱신)
      //    - nearbyCourses / nearbyGhostCourses: stale 마킹만 (현재 페이지에서 refetch 안 함)
      //      → 다음 진입 시 자동 refetch되어 서버 최신 상태 반영
      void queryClient.invalidateQueries({ queryKey: ['bookmarkedCourses'] });
      void queryClient.invalidateQueries({ queryKey: ['nearbyCourses'], refetchType: 'none' });
      void queryClient.invalidateQueries({ queryKey: ['nearbyGhostCourses'], refetchType: 'none' });
    } catch {
      // 4. 롤백 — API 실패 시 원래 상태로 되돌림
      setLiked(courseId, currentIsLiked);
      setCount(courseId, currentCount);
      toast.error('다시 시도해주세요.');
    }
  }

  return { isLiked, getLikeCount, toggle };
}
