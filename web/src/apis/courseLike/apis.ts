import axiosClient from '@/shared/libs/axiosClient';
import type { ApiResponse } from '@/shared/types/api.type';
import type { CourseLikeResponse } from './types';

/**
 * 코스 좋아요 API
 * POST /courses/{courseId}/like/{userId}
 */
export const likeCourse = async (
  courseId: number,
  userId: number
): Promise<ApiResponse<CourseLikeResponse>> => {
  const { data } = await axiosClient.post<ApiResponse<CourseLikeResponse>>(
    `/courses/${courseId}/like/${userId}`
  );
  return data;
};
