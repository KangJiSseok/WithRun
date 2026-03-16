// 월 네비게이션 헤더 (< 2026년 3월 >)

import { ChevronLeft, ChevronRight } from 'lucide-react';
import { cn } from '@/shared/utils';

interface CalendarHeaderProps {
  year: number;
  month: number;
  isCurrentMonth: boolean;
  onPrev: () => void;
  onNext: () => void;
}

export function CalendarHeader({
  year,
  month,
  isCurrentMonth,
  onPrev,
  onNext,
}: CalendarHeaderProps) {
  return (
    <div className="flex items-center justify-between px-[var(--space-2)]">
      <button
        type="button"
        onClick={onPrev}
        className="p-2 rounded-full hover:bg-[hsl(var(--surface-subtle))] text-[hsl(var(--text-primary))]"
      >
        <ChevronLeft size={20} />
      </button>

      <h2 className="h3-b text-[hsl(var(--text-primary))]">
        {year}년 {month}월
      </h2>

      <button
        type="button"
        onClick={onNext}
        disabled={isCurrentMonth}
        className={cn(
          'p-2 rounded-full text-[hsl(var(--text-primary))] hover:bg-[hsl(var(--surface-subtle))]',
          isCurrentMonth && 'text-[hsl(var(--text-disabled))] cursor-not-allowed hover:bg-transparent',
        )}
      >
        <ChevronRight size={20} />
      </button>
    </div>
  );
}
