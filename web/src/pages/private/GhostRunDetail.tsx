import { useParams } from 'react-router-dom';

import { GhostRunDetailScreen } from '@/features/GhostRun';
import { useGhostRunCourseDetail } from '@/hooks/GhostRun/useGhostRunCourseDetail';

export function GhostRunDetailPage() {
  const { courseId } = useParams();
  const { courseDetail, isLoading, error } = useGhostRunCourseDetail(
    courseId ? Number(courseId) : undefined
  );

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

  return <GhostRunDetailScreen course={courseDetail} />;
}
