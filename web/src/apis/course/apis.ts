import axiosClient from '@/shared/libs/axiosClient';
import type { ApiResponse } from '@/shared/types/api.type';
import type {
  GetNearbyCoursesParams,
  GetNearbyCoursesData,
  CourseDetail,
  GetCourseSurveysResponse,
} from './types';

/**
 * 사용자 주변 코스 데이터 받는 API
 */
export const getNearbyCoursesApi = async (
  params: GetNearbyCoursesParams
): Promise<ApiResponse<GetNearbyCoursesData>> => {
  const { preferredDistanceMs, ...rest } = params;
  const { data } = await axiosClient.get<ApiResponse<GetNearbyCoursesData>>(
    '/courses/nearby',
    {
      params: rest,
      paramsSerializer: (p) => {
        const sp = new URLSearchParams();
        Object.entries(p).forEach(([key, value]) => {
          if (value !== undefined && value !== null) {
            sp.append(key, String(value));
          }
        });
        preferredDistanceMs.forEach((item, i) => {
          sp.append(`preferredDistanceMs[${i}].min`, String(item.min));
          sp.append(`preferredDistanceMs[${i}].max`, String(item.max));
        });
        return sp.toString();
      },
    }
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
 * 코스 등록 설문 선택지 조회 API (GET /meta/course/register)
 */
export const getCourseSurveys = async (
): Promise<ApiResponse<GetCourseSurveysResponse>> => {
  const { data } = await axiosClient.get<ApiResponse<GetCourseSurveysResponse>>(
    '/meta/course/register'
  );
  return data;
};
