import type { DailySummary } from '@/shared/apis/Calendar';

interface DayDetailProps {
  date: string;
  summary: DailySummary;
}

interface StatItemProps {
  label: string;
  value: string;
  unit: string;
}

function StatItem({ label, value, unit }: StatItemProps) {
  return (
    <div className="flex-1 flex flex-col items-center gap-[var(--space-1)] bg-[hsl(var(--surface-subtle))] rounded-[var(--radius-md)] py-[var(--space-2)] px-[var(--space-1)]">
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

// 페이스: 1km를 달리는 데 걸리는 시간
function formatPace(distanceM: number, durationSec: number): string {
  if (distanceM === 0) return `--'--"`;
  const secPerKm = durationSec / (distanceM / 1000);
  const min = Math.floor(secPerKm / 60);
  const sec = Math.round(secPerKm % 60);
  return `${min}'${String(sec).padStart(2, '0')}"`;
}

// "2026-03-15" -> "3월 15일"
function formatDate(dateStr: string): string {
  const parts = dateStr.split('-');
  return `${Number(parts[1])}월 ${Number(parts[2])}일`;
}

export function DayDetail({ date, summary }: DayDetailProps) {
  const distanceKm = (summary.dailyDistanceM / 1000).toFixed(2);
  // dailyDurationSec(초)를 "1:03:30" 형식으로 변환
  const duration = formatDuration(summary.dailyDurationSec);
  // 거리 + 시간으로 "5'30" (km당 5분 30초) 계산
  const pace = formatPace(summary.dailyDistanceM, summary.dailyDurationSec);

  return (
    <div className="rounded-[var(--radius-xl)] border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] p-[var(--space-4)] shadow-[var(--shadow-sm)]">
      <p className="body-b text-[hsl(var(--text-primary))] mb-[var(--space-3)]">
        {formatDate(date)} 활동
      </p>
      <div className="flex gap-[var(--space-2)]">
        <StatItem label="거리" value={distanceKm} unit="km" />
        <StatItem label="시간" value={duration} unit="" />
        <StatItem label="페이스" value={pace} unit="/km" />
        <StatItem label="칼로리" value={String(summary.dailyCaloriesKcal)} unit="kcal" />
      </div>
    </div>
  );
}
