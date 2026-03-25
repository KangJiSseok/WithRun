import { useEffect, useState } from 'react';

import { getGhostCourseDetailApi, type GhostCourseDetail } from '@/apis';

// 회원 시스템 구현 전 임시 mock 유저
const MOCK_USER_ID = 1;

export function useGhostRunCourseDetail(courseId: number | undefined) {
  const [courseDetail, setCourseDetail] = useState<GhostCourseDetail | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  useEffect(() => {
    if (!courseId) return;

    setIsLoading(true);
    setError(null);

    getGhostCourseDetailApi(courseId, MOCK_USER_ID)
      .then((res) => setCourseDetail(res.data))
      .catch((err) => setError(err instanceof Error ? err : new Error(String(err))))
      .finally(() => setIsLoading(false));
  }, [courseId]);

  return { courseDetail, isLoading, error };
}
