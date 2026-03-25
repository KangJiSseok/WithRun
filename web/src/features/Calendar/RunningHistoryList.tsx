import { useNavigate } from 'react-router-dom';
import { useInfiniteScroll } from '@/shared/hooks';
import { formatDateLabel } from '@/shared/utils';
import type { RunningHistoryItem } from '@/apis/runningSessions';

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

// 날짜 순서를 유지하면서 날짜별로 그룹핑
function groupByDate(items: RunningHistoryItem[]): { dateKey: string; sessions: RunningHistoryItem[] }[] {
  const groups: { dateKey: string; sessions: RunningHistoryItem[] }[] = [];
  for (const item of items) {
    // 1. ISO 문자열에서 날짜만 추출 ("2026-03-16")
    const dateKey = item.startedAt.slice(0, 10);
    // 2. 마지막 그룹과 날짜 비교
    const last = groups[groups.length - 1];
    // 3. 같은 날짜면 기존 그룹에 추가, 다른 날짜면 새 그룹 생성
    if (last?.dateKey === dateKey) {
      last.sessions.push(item);
    } else {
      groups.push({ dateKey, sessions: [item] });
    }
  }
  return groups;
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
  items: RunningHistoryItem[];
  isLoading: boolean;
  hasMore: boolean;
  isScrollLoading: boolean;
  showDateHeader: boolean;
  onLoadMore: () => void;
}

function SessionCard({ session }: { session: RunningHistoryItem }) {
  const navigate = useNavigate();

  return (
    <button
      type="button"
      onClick={() => navigate(`/calendar/${session.runningSessionId}`)}
      className="w-full text-left rounded-[var(--radius-xl)] border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] p-[var(--space-3)] flex gap-[var(--space-3)] items-center active:opacity-70 transition-opacity">
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
      <div className="flex-1 flex flex-col gap-[var(--space-2)]">
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
        <div className="flex items-center body-r text-[hsl(var(--text-secondary))]">
          <span className="flex-1 text-center">{(session.distanceM / 1000).toFixed(2)} km</span>
          <span className="flex-shrink-0 px-[var(--space-2)]">•</span>
          <span className="flex-1 text-center">{formatDuration(session.durationSec)}</span>
          <span className="flex-shrink-0 px-[var(--space-2)]">•</span>
          <span className="flex-1 text-center">{formatPace(session.distanceM, session.durationSec)}</span>
        </div>
      </div>
    </button>
  );
}

export function RunningHistoryList({ title, items, isLoading, hasMore, isScrollLoading, showDateHeader, onLoadMore }: RunningHistoryListProps) {
  // 목록 끝에 위치한 sentinel 요소가 뷰포트에 들어오면 자동으로 다음 페이지 로드
  const sentinelRef = useInfiniteScroll(hasMore, isScrollLoading, onLoadMore);

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

      {!isLoading && showDateHeader
        ? groupByDate(items).map(({ dateKey, sessions }) => (
            <div key={dateKey} className="flex flex-col gap-[var(--space-2)]">
              <p className="caption-b text-[hsl(var(--text-secondary))]">{formatDateLabel(dateKey)}</p>
              {sessions.map((session) => (
                <SessionCard key={session.runningSessionId} session={session} />
              ))}
            </div>
          ))
        : !isLoading && items.map((session) => (
            <SessionCard key={session.runningSessionId} session={session} />
          ))}

      {/* 스크롤 감지용 sentinel — 뷰포트 진입 시 다음 페이지 자동 로드 */}
      <div ref={sentinelRef} />

      {isScrollLoading && (
        <div className="flex justify-center py-[var(--space-2)]">
          <span className="caption-r text-[hsl(var(--text-secondary))]">불러오는 중...</span>
        </div>
      )}
    </div>
  );
}
