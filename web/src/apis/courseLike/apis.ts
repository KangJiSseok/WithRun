import axiosClient from '@/shared/libs/axiosClient';
import type { ApiResponse } from '@/shared/types/api.type';
import type { CourseLikeResponse } from './types';

/**
 * 코스 좋아요 API
 * POST /courses/{courseId}/like
 */
export const likeCourse = async (
  courseId: number,
): Promise<ApiResponse<CourseLikeResponse>> => {
  const { data } = await axiosClient.post<ApiResponse<CourseLikeResponse>>(
    `/courses/${courseId}/like`
  );
  return data;
};

/**
 * 코스 좋아요 취소 API
 * DELETE /courses/{courseId}/like
 */
export const unlikeCourse = async (
  courseId: number,
): Promise<ApiResponse<CourseLikeResponse>> => {
  const { data } = await axiosClient.delete<ApiResponse<CourseLikeResponse>>(
    `/courses/${courseId}/like`
  );
  return data;
};
