import axiosClient from '@/shared/libs/axiosClient';
import type { ApiResponse } from '@/shared/types/api.type';
import type { CreateRunningSessionRequest, RunningSessionData, RegisterCourseRequest, RegisterCourseResponse } from './types';

/**
 * 러닝 세션 생성 API
 * courseId 입력 : 일반 러닝
 * ghostTargetRunningSessionId 입력 : 고스트런 러닝
 * courseId, ghostTargetRunningSessionId 둘 다 입력 X : 자유 러닝
 */
export const createRunningSession = async (
  body: CreateRunningSessionRequest
): Promise<ApiResponse<RunningSessionData>> => {
  const { data } = await axiosClient.post<ApiResponse<RunningSessionData>>(
    '/running-sessions',
    body
  );
  return data;
};

/**
 * 러닝 기록을 공식 코스로 등록하는 API
 * POST /running-sessions/{runningSessionId}/register-course
 */
export const registerCourse = async (
  runningSessionId: number,
  body: RegisterCourseRequest
): Promise<ApiResponse<RegisterCourseResponse>> => {
  const { data } = await axiosClient.post<ApiResponse<RegisterCourseResponse>>(
    `/running-sessions/${runningSessionId}/register-course`,
    body
  );
  return data;
};
