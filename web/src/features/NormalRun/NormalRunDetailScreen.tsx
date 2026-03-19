import { ArrowLeft, MapPin } from 'lucide-react';
import { useEffect, useRef, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';

import { Badge } from '@/shared/components/shadcn/badge';
import { Button } from '@/shared/components/shadcn/button';
import { useKakaoMap } from '@/shared/hooks/useKakaoMap';
import type { CourseDetail } from '@/apis/courses';
import type { CourseDifficulty, CourseType } from '@/shared/types/api.type';

const DIFFICULTY_LABEL: Record<CourseDifficulty, string> = {
  EASY: '쉬움',
  MEDIUM: '보통',
  HARD: '어려움',
};

const COURSE_TYPE_LABEL: Record<CourseType, string> = {
  URBAN: '도심',
  PARK: '공원',
  RIVERSIDE: '한강변',
  MOUNTAIN_TRAIL: '산/둘레길',
  TRACK: '트랙',
  OTHER: '기타',
};

type NormalRunDetailScreenProps = {
  course: CourseDetail;
};

export function NormalRunDetailScreen({ course }: NormalRunDetailScreenProps) {
  const navigate = useNavigate();
  const { courseId } = useParams();
  const containerRef = useRef<HTMLDivElement>(null);
  const [isAtStartPoint, setIsAtStartPoint] = useState(false);

  // TODO: 실제 GPS 출발지 도착 감지 로직으로 교체
  useEffect(() => {
    const timer = setTimeout(() => setIsAtStartPoint(true), 2000);
    return () => clearTimeout(timer);
  }, []);

  // coordinates를 전달하면 폴리라인이 자동으로 그려지고 경로 전체가 보이도록 지도 범위 조정
  useKakaoMap(containerRef, { coordinates: course.coordinates });

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
            {(course.distanceM / 1000).toFixed(1)}km
          </Badge>
          <Badge
            variant="outline"
            className="rounded-full border-[hsl(var(--border-default))] bg-transparent px-[var(--space-3)] py-[var(--space-1)]"
          >
            고도 {course.elevationGainM}m
          </Badge>
          {course.difficulty != null && (
            <Badge
              variant="outline"
              className="rounded-full border-[hsl(var(--border-default))] bg-transparent px-[var(--space-3)] py-[var(--space-1)]"
            >
              {DIFFICULTY_LABEL[course.difficulty]}
            </Badge>
          )}
          {course.types.map((type) => (
            <Badge
              key={type}
              variant="outline"
              className="rounded-full border-[hsl(var(--primary-1)/0.25)] bg-[hsl(var(--primary-1)/0.1)] px-[var(--space-3)] py-[var(--space-1)] text-[hsl(var(--primary-1))]"
            >
              {COURSE_TYPE_LABEL[type]}
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
          onClick={() => navigate(`/normal/${courseId}/running`)}
          className="h-14 w-full rounded-[var(--radius-xl)] body-l-b"
        >
          러닝 시작
        </Button>
      </div>
    </div>
  );
}
