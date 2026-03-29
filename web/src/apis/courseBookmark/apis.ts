import axiosClient from '@/shared/libs/axiosClient';
import type { ApiResponse } from '@/shared/types/api.type';
import type { GetBookmarkedCoursesParams, GetBookmarkedCoursesData, AddBookmarkData } from './types';

/**
 * 북마크한 코스 목록 조회
 * GET /courses/bookmarks
 */
export const getBookmarkedCoursesApi = async (
  params: GetBookmarkedCoursesParams = {},
): Promise<ApiResponse<GetBookmarkedCoursesData>> => {
  const { data } = await axiosClient.get<ApiResponse<GetBookmarkedCoursesData>>(
    '/courses/bookmarks',
    { params },
  );
  return data;
};

/**
 * 코스 북마크 등록
 * POST /courses/{courseId}/bookmarks
 */
export const addBookmarkApi = async (courseId: number): Promise<ApiResponse<AddBookmarkData>> => {
  const { data } = await axiosClient.post<ApiResponse<AddBookmarkData>>(
    `/courses/${courseId}/bookmark`,
  );
  return data;
};

/**
 * 코스 북마크 해제
 * DELETE /courses/{courseId}/bookmarks
 */
export const deleteBookmarkApi = async (courseId: number): Promise<void> => {
  await axiosClient.delete(`/courses/${courseId}/bookmark`);
};
