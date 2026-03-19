import axiosClient from '@/shared/libs/axiosClient';
import type { ApiResponse } from '@/shared/types/api.type';
import type {
  CreateRunningSessionRequest,
  RunningSessionData,
  CompleteRunningSessionRequest,
  CompleteRunningSessionResponse,
  RegisterCourseRequest,
  RegisterCourseResponse,
  GetRunningHistoryResponse,
  GetRunningHistoryParams,
} from './types';

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

// 회원 시스템 구현 전 임시 mock 유저
// TODO : 추후 변경 시 => auth context에서 실제 유저 정보 받아오기
const MOCK_USER_ID = 1;

/**
 * 과거 러닝 세션 기록 조회
 * GET /running-sessions/history/past
 */
export const getRunningHistoryApi = async (
  params: GetRunningHistoryParams = {}
): Promise<ApiResponse<GetRunningHistoryResponse>> => {
  const { data } = await axiosClient.get<
    ApiResponse<GetRunningHistoryResponse>
  >('/running-sessions/history/past', {
    params: { userId: MOCK_USER_ID, ...params },
  });
  return data;
};

/**
 * 러닝 세션 종료 API
 * PATCH /running-sessions/{runningSessionId}
 */
export const completeRunningSession = async (
  runningSessionId: number,
  body: CompleteRunningSessionRequest,
): Promise<ApiResponse<CompleteRunningSessionResponse>> => {
  const { data } = await axiosClient.patch<ApiResponse<CompleteRunningSessionResponse>>(
    `/running-sessions/${runningSessionId}`,
    body,
  );
  return data;
};

/**
 * 러닝 세션 종료 API
 * PATCH /running-sessions/{runningSessionId}
 */
export const patchCompleteRunningSessionApi = async (
  runningSessionId: number,
  body: CompleteRunningSessionRequest,
): Promise<ApiResponse<CompleteRunningSessionResponse>> => {
  const { data } = await axiosClient.patch<ApiResponse<CompleteRunningSessionResponse>>(
    `/running-sessions/${runningSessionId}`,
    body,
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
