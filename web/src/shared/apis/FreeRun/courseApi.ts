import axiosClient from '@/shared/libs/axiosClient';

// 추후 변경: auth context에서 실제 userId 받아오기
const MOCK_USER_ID = 1;

interface CourseLikeResponse {
  courseId: number;
  isLiked: boolean;
  likeCount: number;
  status: 'OFFICIAL' | 'COMMUNITY' | 'PRIVATE';
}

export async function likeCourse(courseId: number): Promise<CourseLikeResponse> {
  const res = await axiosClient.post<{ data: CourseLikeResponse }>(
    `/api/courses/${courseId}/like/${MOCK_USER_ID}`,
  );
  return res.data.data;
}
