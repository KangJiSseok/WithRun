import { ArrowLeft, MapPin } from 'lucide-react';
import { useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';

import { Badge } from '@/shared/components/shadcn/badge';
import { Button } from '@/shared/components/shadcn/button';
import { useKakaoMap } from '@/shared/hooks/useKakaoMap';
import type { RunningCourse } from '@/shared/components/RunningCourseCard';

type NormalRunDetailScreenProps = {
  course: RunningCourse;
};

export function NormalRunDetailScreen({ course }: NormalRunDetailScreenProps) {
  const navigate = useNavigate();
  const containerRef = useRef<HTMLDivElement>(null);
  const [isAtStartPoint] = useState(false);

  useKakaoMap(containerRef);

  return (
    <div className="relative h-screen w-full">
      {/* 지도 */}
      <div ref={containerRef} className="h-full w-full" />

      {/* 상단 뒤로가기 버튼 */}
      <div className="absolute left-[var(--space-4)] top-[var(--space-4)] z-raised">
        <Button
          type="button"
          variant="outline"
          size="icon"
          onClick={() => navigate(-1)}
          className="h-12 w-12 rounded-full border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] text-[hsl(var(--text-primary))]"
        >
          <ArrowLeft size={20} />
        </Button>
      </div>

      {/* 하단 코스 정보 패널 */}
      <div className="absolute bottom-0 left-0 right-0 z-raised flex flex-col gap-[var(--space-4)] rounded-t-[var(--radius-2xl)] bg-[hsl(var(--surface-default))] p-[var(--space-5)] shadow-[var(--shadow-lg)]">
        <h2 className="h2-b text-[hsl(var(--text-primary))]">{course.title}</h2>

        {/* 코스 정보 배지 */}
        <div className="flex flex-wrap gap-[var(--space-2)]">
          <Badge
            variant="outline"
            className="rounded-full border-[hsl(var(--border-default))] bg-transparent px-[var(--space-3)] py-[var(--space-1)]"
          >
            {course.distance}
          </Badge>
          <Badge
            variant="outline"
            className="rounded-full border-[hsl(var(--border-default))] bg-transparent px-[var(--space-3)] py-[var(--space-1)]"
          >
            고도 {course.elevation}
          </Badge>
          <Badge
            variant="outline"
            className="rounded-full border-[hsl(var(--border-default))] bg-transparent px-[var(--space-3)] py-[var(--space-1)]"
          >
            {course.difficulty}
          </Badge>
          {course.courseType.map((type) => (
            <Badge
              key={type}
              variant="outline"
              className="rounded-full border-[hsl(var(--primary-1)/0.25)] bg-[hsl(var(--primary-1)/0.1)] px-[var(--space-3)] py-[var(--space-1)] text-[hsl(var(--primary-1))]"
            >
              {type}
            </Badge>
          ))}
        </div>

        {/* 출발 지점 도착 안내 박스 */}
        <div className="flex items-start gap-[var(--space-3)] rounded-[var(--radius-lg)] border border-[hsl(var(--border-default))] bg-[hsl(var(--primary-1)/0.05)] p-[var(--space-4)]">
          <MapPin
            size={20}
            className="mt-0.5 shrink-0 text-[hsl(var(--primary-1))]"
          />
          <div className="flex flex-col gap-[var(--space-1)]">
            <p className="body-l-b text-[hsl(var(--text-primary))]">
              출발 지점 도착
            </p>
            <p className="body-r text-[hsl(var(--text-secondary))]">
              출발 지점에 도착하면 러닝이 시작됩니다.
            </p>
          </div>
        </div>

        {/* 러닝 시작 버튼 */}
        <Button
          type="button"
          disabled={!isAtStartPoint}
          className="h-14 w-full rounded-[var(--radius-xl)] body-l-b"
        >
          러닝 시작
        </Button>
      </div>
    </div>
  );
}
