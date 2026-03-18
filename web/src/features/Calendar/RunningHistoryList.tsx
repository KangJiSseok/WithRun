import type { RunningSession } from '@/shared/apis/Calendar';

const TIME_SLOT_LABEL: Record<string, string> = {
  DAWN: '새벽',
  MORNING: '오전',
  AFTERNOON: '오후',
  EVENING: '저녁',
};

// "2026-03-16T13:05:00" -> "13:05"
function formatTime(startedAt: string): string {
  const date = new Date(startedAt);
  return `${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`;
}

// 초 -> "m:ss" (예: 1930 -> "32:10")
function formatDuration(sec: number | null): string {
  if (sec === null) return '--';
  const m = Math.floor(sec / 60);
  const s = sec % 60;
  return `${m}:${String(s).padStart(2, '0')}`;
}

// 페이스: 초/미터 -> "M'SS\"/km" (예: 6'11"/km)
function formatPace(distanceM: number, durationSec: number | null): string {
  if (!durationSec || distanceM === 0) return '--';
  const paceSecPerKm = (durationSec / distanceM) * 1000;
  let paceMin = Math.floor(paceSecPerKm / 60);
  let paceSec = Math.round(paceSecPerKm % 60);
  if (paceSec === 60) {
    paceMin += 1;
    paceSec = 0;
  }
  return `${paceMin}'${String(paceSec).padStart(2, '0')}"/km`;
}

interface RunningHistoryListProps {
  title: string;
  items: RunningSession[];
  isLoading: boolean;
  hasMore: boolean;
  isLoadingMore: boolean;
  onLoadMore: () => void;
}

export function RunningHistoryList({ title, items, isLoading, hasMore, isLoadingMore, onLoadMore }: RunningHistoryListProps) {
  return (
    <div className="flex flex-col gap-[var(--space-3)]">
      <p className="body-b text-[hsl(var(--text-primary))]">{title}</p>

      {isLoading && (
        <div className="rounded-[var(--radius-xl)] border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] p-[var(--space-4)] flex justify-center">
          <span className="body-r text-[hsl(var(--text-secondary))]">로딩 중...</span>
        </div>
      )}

      {!isLoading && items.length === 0 && (
        <div className="rounded-[var(--radius-xl)] border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] p-[var(--space-4)] text-center">
          <p className="body-r text-[hsl(var(--text-secondary))]">러닝 기록이 없습니다.</p>
        </div>
      )}

      {!isLoading &&
        items.map((session) => (
          <div
            key={session.runningSessionId}
            className="rounded-[var(--radius-xl)] border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] p-[var(--space-3)] flex gap-[var(--space-3)] items-center"
          >
            {/* 스냅샷 썸네일 */}
            <div className="w-[var(--space-16)] h-[var(--space-16)] rounded-[var(--radius-lg)] bg-[hsl(var(--surface-subtle))] flex-shrink-0 overflow-hidden">
              {session.snapshotImageUrl ? (
                <img
                  src={session.snapshotImageUrl}
                  alt="러닝 스냅샷"
                  className="w-full h-full object-cover"
                />
              ) : (
                <div className="w-full h-full" />
              )}
            </div>

            {/* 러닝 정보 */}
            <div className="flex-1 flex flex-col gap-[var(--space-1)]">
              {/* 시간대 + 시각 */}
              <div className="flex justify-between items-center">
                <span className="body-b text-[hsl(var(--text-primary))]">
                  {TIME_SLOT_LABEL[session.timeSlot] ?? session.timeSlot} 러닝
                </span>
                <span className="caption-r text-[hsl(var(--text-secondary))]">
                  {formatTime(session.startedAt)}
                </span>
              </div>

              {/* 거리 • 시간 • 페이스 */}
              <p className="caption-r text-[hsl(var(--text-secondary))]">
                {(session.distanceM / 1000).toFixed(2)} km
                {' • '}
                {formatDuration(session.durationSec)}
                {' • '}
                {formatPace(session.distanceM, session.durationSec)}
              </p>
            </div>
          </div>
        ))}

      {!isLoading && hasMore && (
        // 클릭 시 onLoadMore 호출
        // 로딩 중엔 비활성화 + "불러오는 중..." 텍스트 표시
        <button
          type="button"
          onClick={onLoadMore}
          disabled={isLoadingMore}
          className="w-full p-[var(--space-3)] rounded-[var(--radius-xl)] border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] body-r text-[hsl(var(--text-secondary))] hover:bg-[hsl(var(--surface-subtle))] transition-colors disabled:opacity-50"
        >
          {isLoadingMore ? '불러오는 중...' : '더 보기'}
        </button>
      )}
    </div>
  );
}
