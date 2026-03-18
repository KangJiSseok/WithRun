import { Bookmark, Heart, MapPin } from 'lucide-react';
import type { MouseEvent } from 'react';

import { Badge } from '@/shared/components/shadcn/badge';
import { Button } from '@/shared/components/shadcn/button';
import { Card, CardContent } from '@/shared/components/shadcn/card';

export type RunningCourseTab = 'official' | 'community';

export type RunningCourse = {
  id: number;
  title: string;
  distance: string;
  elevation: string;
  difficulty: string;
  purpose: string[];
  likes: number;
  courseType: string[];
  path: string;
  color: string;
  tab: RunningCourseTab;
};

type RunningCourseCardProps = {
  course: RunningCourse;
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
    onToggleSave(course.id);
  }

  return (
    <Card
      role={onClick ? 'button' : undefined}
      onClick={onClick}
      className="overflow-hidden py-0 rounded-[var(--radius-xl)] border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] shadow-[var(--shadow-md)]"
    >
      <div className="relative h-44 overflow-hidden">
        <img
          src="https://placehold.co/430x176/e8e8e8/cccccc"
          alt={course.title}
          className="absolute inset-0 h-full w-full object-cover"
        />

        <div className="absolute left-5 top-5 z-raised">
          <Badge className="rounded-full bg-[hsl(var(--surface-default))] px-[var(--space-3)] py-[var(--space-2)]">
            <MapPin
              size={14}
              className="mr-[var(--space-1)] text-[hsl(var(--primary-1))]"
            />
            {course.distance}
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
          <div className="flex flex-col gap-[var(--space-2)]">
            <h3 className="h3-b text-[hsl(var(--text-primary))]">
              {course.title}
            </h3>
          </div>

          <div className="flex items-center gap-[var(--space-1)] text-[hsl(var(--primary-1))]">
            <Heart size={18} fill="currentColor" />
            <span className="body-l-b">{course.likes.toLocaleString()}</span>
          </div>
        </div>

        <div className="flex flex-wrap gap-[var(--space-2)]">
          <Badge
            variant="outline"
            className="rounded-full border-[hsl(var(--border-default))] bg-transparent px-[var(--space-3)] py-[var(--space-1)]"
          >
            {course.difficulty}
          </Badge>
          <Badge
            variant="outline"
            className="rounded-full border-[hsl(var(--border-default))] bg-transparent px-[var(--space-3)] py-[var(--space-1)]"
          >
            고도 {course.elevation}
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
      </CardContent>
    </Card>
  );
}
