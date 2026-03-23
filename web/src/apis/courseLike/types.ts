import type { CourseStatus } from '@/shared/types/api.type';

// 코스 좋아요 응답 (POST /courses/{courseId}/like/{userId})
export type CourseLikeResponse = {
  courseId: number;
  isLiked: boolean;
  likeCount: number;
  status: CourseStatus;
};
