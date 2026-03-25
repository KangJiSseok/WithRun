import { useParams } from 'react-router-dom';
import { useCalendarDetail } from '@/hooks/Calendar';
import { CalendarDetailScreen } from '@/features/Calendar';

export function CalendarDetailPage() {
  const { runningSessionId } = useParams();
  const { detail, isLoading, isError } = useCalendarDetail(Number(runningSessionId));

  if (isLoading) {
    return (
      <div className="flex h-screen items-center justify-center">
        <p className="body-r text-[hsl(var(--text-secondary))]">불러오는 중...</p>
      </div>
    );
  }

  if (isError || !detail) {
    return (
      <div className="flex h-screen items-center justify-center">
        <p className="body-r text-[hsl(var(--text-secondary))]">기록을 불러올 수 없습니다.</p>
      </div>
    );
  }

  return <CalendarDetailScreen detail={detail} />;
}
