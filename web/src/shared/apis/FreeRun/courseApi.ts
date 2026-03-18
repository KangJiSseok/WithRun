import axiosClient from '@/shared/libs/axiosClient';

// 추후 변경: auth context에서 실제 userId 받아오기
const MOCK_USER_ID = 1;

// 추후 변경: 러닝 종료 시 POST /api/runs 응답에서 받아온 runningSessionId를 store에서 읽어오기
export const MOCK_RUNNING_SESSION_ID = 17;

// 코스 등록 시 필요한 설문조사 선택지 API
// API 명세서에서 난이도와 코스 타입은 label과 index로 옴
export interface SurveyOption {
  index: number;
  label: string;
}

export interface CourseSurveyData {
  courseTypes: SurveyOption[];
  difficulties: SurveyOption[];
}

export async function fetchCourseSurveys(): Promise<CourseSurveyData> {
  // HTTP 요청이라 응답 올 때까지 대기 필요 -> async/await 사용
  // { data: CourseSurveyData }: 응답 body의 타입 지정
  const res = await axiosClient.get<{ data: CourseSurveyData }>('/courses/surveys');
  return res.data.data;
}

interface CourseLikeResponse {
  courseId: number;
  isLiked: boolean;
  likeCount: number;
  status: 'OFFICIAL' | 'COMMUNITY' | 'PRIVATE';
}

interface RegisterCourseRequest {
  title: string;
  difficulty: string;
  courseTypes: string[];
  snapshotImageUrl?: string;
}

interface RegisterCourseResponse {
  courseId: number;
  title: string;
  status: string;
  difficulty: string;
  distanceM: number;
  elevationGainM: number;
  snapshotImageUrl: string | null;
  startLatitude: number;
  startLongitude: number;
  endLatitude: number | null;
  endLongitude: number | null;
  coordinates: { latitude: number; longitude: number; elevation: number }[];
}

export async function registerCourse(
  runningSessionId: number,
  body: RegisterCourseRequest,
): Promise<RegisterCourseResponse> {
  const res = await axiosClient.post<{ data: RegisterCourseResponse }>(
    `/running-sessions/${runningSessionId}/register-course`,
    body,
  );
  return res.data.data;
}

export async function likeCourse(courseId: number): Promise<CourseLikeResponse> {
  const res = await axiosClient.post<{ data: CourseLikeResponse }>(
    `/courses/${courseId}/like/${MOCK_USER_ID}`,
  );
  return res.data.data;
}
