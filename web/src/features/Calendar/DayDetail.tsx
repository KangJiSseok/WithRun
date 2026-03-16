import type { DailySummary } from '@/shared/apis/Calendar';

interface DayDetailProps {
  date: string;
  // 같은 날 여러 운동 기록을 배열로 받음
  summaries: DailySummary[];
}

interface StatItemProps {
  label: string;
  value: string;
  unit: string;
}

function StatItem({ label, value, unit }: StatItemProps) {
  return (
    <div className="flex-1 flex flex-col items-center gap-[var(--space-2)] bg-[hsl(var(--surface-subtle))] rounded-[var(--radius-md)] py-[var(--space-3)] px-[var(--space-2)]">
      <span className="caption-r text-[hsl(var(--text-secondary))]">{label}</span>
      <span className="body-b text-[hsl(var(--text-primary))]">{value}</span>
      {unit && <span className="caption-r text-[hsl(var(--text-secondary))]">{unit}</span>}
    </div>
  );
}

// 시간 x:xx:xx
function formatDuration(sec: number): string {
  const h = Math.floor(sec / 3600);
  const m = Math.floor((sec % 3600) / 60);
  const s = sec % 60;
  if (h > 0) {
    return `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  }
  return `${m}:${String(s).padStart(2, '0')}`;
}

// "2026-03-15" -> "3월 15일"
function formatDate(dateStr: string): string {
  const parts = dateStr.split('-');
  return `${Number(parts[1])}월 ${Number(parts[2])}일`;
}

export function DayDetail({ date, summaries }: DayDetailProps) {
  return (
    <div className="flex flex-col gap-[var(--space-6)]">
      {/* 날짜 헤더: 카드 밖에 표시 */}
      <p className="body-b text-[hsl(var(--text-primary))] px-[var(--space-1)]">
        {formatDate(date)} 활동
      </p>
      {/* 기록별 개별 카드 */}
      {summaries.map((summary, idx) => {
        const distanceKm = (summary.dailyDistanceM / 1000).toFixed(2);
        const duration = formatDuration(summary.dailyDurationSec);
        const calories = String(summary.dailyCaloriesKcal);
        return (
          <div
            key={idx}
            className="rounded-[var(--radius-xl)] border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] p-[var(--space-4)] shadow-[var(--shadow-sm)]"
          >
            <div className="flex gap-[var(--space-3)]">
              <StatItem label="거리" value={distanceKm} unit="km" />
              <StatItem label="시간" value={duration} unit="" />
              <StatItem label="칼로리" value={calories} unit="kcal" />
            </div>
          </div>
        );
      })}
    </div>
  );
}
