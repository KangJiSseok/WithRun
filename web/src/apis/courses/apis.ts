import axiosClient from '@/shared/libs/axiosClient';
import type { ApiResponse } from '@/shared/types/apiResponse.type';
import type { GetNearbyCoursesParams, GetNearbyCoursesData } from './types';

/**
 * 사용자 주변 코스 데이터 받는 API
 */
export const getNearbyCoursesApi = async (
  params: GetNearbyCoursesParams
): Promise<ApiResponse<GetNearbyCoursesData>> => {
  const { data } = await axiosClient.get<ApiResponse<GetNearbyCoursesData>>(
    '/api/courses/nearby',
    { params }
  );
  return data;
};
