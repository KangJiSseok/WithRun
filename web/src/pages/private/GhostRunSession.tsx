import { useParams } from 'react-router-dom';

import { GhostRunSessionScreen, GhostRunResultScreen } from '@/features/GhostRun';
import { useGhostRunCourseDetail } from '@/hooks/GhostRun/useGhostRunCourseDetail';
import { useGhostRunSessionStore } from '@/stores/GhostRun';

export function GhostRunSessionPage() {
  const { courseId } = useParams();
  const { courseDetail, isLoading, error } = useGhostRunCourseDetail(
    courseId ? Number(courseId) : undefined
  );
  const result = useGhostRunSessionStore((s) => s.result);

  if (isLoading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <p className="body-l text-[hsl(var(--text-secondary))]">
          코스 정보를 불러오는 중입니다...
        </p>
      </div>
    );
  }

  if (error || !courseDetail) {
    return (
      <div className="flex h-screen items-center justify-center">
        <p className="body-l text-[hsl(var(--text-secondary))]">
          코스를 찾을 수 없습니다.
        </p>
      </div>
    );
  }

  if (result) {
    return <GhostRunResultScreen result={result} />;
  }

  return <GhostRunSessionScreen courseDetail={courseDetail} />;
}
