import axiosClient from '@/shared/libs/axiosClient';

// 추후 변경: auth context에서 실제 userId 받아오기
const MOCK_USER_ID = 1;

// 추후 변경: 러닝 종료 시 POST /api/runs 응답에서 받아온 runningSessionId를 store에서 읽어오기
export const MOCK_RUNNING_SESSION_ID = 17;


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
