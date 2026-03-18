import { useParams } from 'react-router-dom';

import { NormalRunDetailScreen } from '@/features/NormalRun';
import { COURSES } from '@/constants/NormalRun';

export function NormalRunDetailPage() {
  const { courseId } = useParams();
  const course = COURSES.find((c) => c.id === Number(courseId));

  if (!course) {
    return (
      <div className="flex h-screen items-center justify-center">
        <p className="body-l text-[hsl(var(--text-secondary))]">
          코스를 찾을 수 없습니다.
        </p>
      </div>
    );
  }

  return <NormalRunDetailScreen course={course} />;
}
