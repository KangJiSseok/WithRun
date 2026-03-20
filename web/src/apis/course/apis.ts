import axiosClient from '@/shared/libs/axiosClient';
import type { ApiResponse } from '@/shared/types/api.type';
import type {
  GetNearbyCoursesParams,
  GetNearbyCoursesData,
  CourseDetail,
  GetCourseSurveysResponse,
  GetNormalRunCourseFilterData,
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

/**
 * 코스 필터 선택지 조회 API (GET /courses/surveys)
 */
export const getNormalRunCourseFilterApi = async (): Promise<
  ApiResponse<GetNormalRunCourseFilterData>
> => {
  const { data } = await axiosClient.get('/courses/filter/normal-run');
  return data;
};

/**
 * 코스 설문 선택지 조회 API (GET /courses/surveys)
 */
export const getCourseSurveys = async (): Promise<
  ApiResponse<GetCourseSurveysResponse>
> => {
  const { data } =
    await axiosClient.get<ApiResponse<GetCourseSurveysResponse>>(
      '/courses/surveys'
    );
  return data;
};
