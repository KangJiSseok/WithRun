import axiosClient from '@/shared/libs/axiosClient';
import type { ApiResponse } from '@/shared/types/api.type';
import type {
  GetNearbyCoursesParams,
  GetNearbyCoursesData,
  CourseDetail,
} from './types';

/**
 * 사용자 주변 코스 데이터 받는 API
 */
export const getNearbyCoursesApi = async (
  params: GetNearbyCoursesParams
): Promise<ApiResponse<GetNearbyCoursesData>> => {
  const { data } = await axiosClient.get<ApiResponse<GetNearbyCoursesData>>(
    '/courses/nearby',
    { params }
  );
  return data;
};

/**
 * 코스 상세 데이터 받는 API
 */
export const getCourseDetailApi = async (
  courseId: number
): Promise<ApiResponse<CourseDetail>> => {
  const { data } = await axiosClient.get<ApiResponse<CourseDetail>>(
    `/courses/${courseId}`
  );
  return data;
};

