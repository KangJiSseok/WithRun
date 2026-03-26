import { Bookmark, Heart, MapPin } from 'lucide-react';
import type { MouseEvent } from 'react';

import { Badge } from '@/shared/components/shadcn/badge';
import { Button } from '@/shared/components/shadcn/button';
import { Card, CardContent } from '@/shared/components/shadcn/card';
import type { CourseItem } from '@/apis/course';

type RunningCourseCardProps = {
  course: CourseItem;
  isSaved: boolean;
  onClick?: () => void;
  onToggleSave: (courseId: number) => void;
};

export function RunningCourseCard({
  course,
  isSaved,
  onClick,
  onToggleSave,
}: RunningCourseCardProps) {
  function handleToggleSave(event: MouseEvent<HTMLButtonElement>) {
    event.stopPropagation();
    onToggleSave(course.courseId);
  }

  return (
    <Card
      role={onClick ? 'button' : undefined}
      onClick={onClick}
      className="overflow-hidden py-0 rounded-[var(--radius-xl)] border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] shadow-[var(--shadow-md)]"
    >
      <div className="relative h-44 overflow-hidden">
        <img
          src={course.snapshotImageUrl || '/course-fallback.svg'}
          alt={course.title}
          draggable={false}
          className="absolute inset-0 h-full w-full object-fit"
        />

        <div className="absolute left-5 top-5 z-raised">
          <Badge className="rounded-full bg-[hsl(var(--surface-default))] px-[var(--space-3)] py-[var(--space-2)]">
            <MapPin
              size={14}
              className="mr-[var(--space-1)] text-[hsl(var(--primary-1))]"
            />
            {(course.distanceM / 1000).toFixed(1)}km
          </Badge>
        </div>

        <div className="absolute right-5 top-5 z-raised">
          <Button
            type="button"
            variant="outline"
            size="icon"
            onClick={handleToggleSave}
            className="h-11 w-11 rounded-full border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] text-[hsl(var(--text-primary))]"
          >
            <Bookmark
              size={18}
              className={
                isSaved
                  ? 'fill-[hsl(var(--primary-1))] text-[hsl(var(--primary-1))]'
                  : ''
              }
            />
          </Button>
        </div>
      </div>

      <CardContent className="flex flex-col gap-[var(--space-4)] p-[var(--space-5)]">
        <div className="flex items-start justify-between gap-[var(--space-4)]">
          <h3 className="h3-b text-[hsl(var(--text-primary))]">
            {course.title}
          </h3>

          <div className="flex items-center gap-[var(--space-1)] text-[hsl(var(--primary-1))]">
            <Heart size={18} fill="currentColor" />
            <span className="body-l-b">
              {course.likeCount.toLocaleString()}
            </span>
          </div>
        </div>

        <div className="flex flex-wrap gap-[var(--space-2)]">
          {course.difficulty != null && (
            <Badge
              variant="outline"
              className="rounded-full border-[hsl(var(--border-default))] bg-transparent px-[var(--space-3)] py-[var(--space-1)]"
            >
              {course.difficulty.label}
            </Badge>
          )}
          <Badge
            variant="outline"
            className="rounded-full border-[hsl(var(--border-default))] bg-transparent px-[var(--space-3)] py-[var(--space-1)]"
          >
            고도 {course.elevationGainM}m
          </Badge>
          {course.courseTypes.map((type) => (
            <Badge
              key={type.data}
              variant="outline"
              className="rounded-full border-[hsl(var(--primary-1)/0.25)] bg-[hsl(var(--primary-1)/0.1)] px-[var(--space-3)] py-[var(--space-1)] text-[hsl(var(--primary-1))]"
            >
              {type.label}
            </Badge>
          ))}
        </div>
      </CardContent>
    </Card>
  );
}
