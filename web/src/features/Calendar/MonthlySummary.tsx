// 월별 통계 요약
// 러닝 횟수, 총 거리 (km), 소모 칼로리
// data가 null이면 카드 틀은 유지하고 값만 '--' 로 표시
interface MonthlySummaryProps {
  runCount: number | null;
  distanceM: number | null;
  caloriesKcal: number | null;
}

interface StatCardProps {
  label: string;
  value: string;
  unit: string;
}

function StatCard({ label, value, unit }: StatCardProps) {
  return (
    <div className="flex-1 flex flex-col items-center gap-[var(--space-1)] bg-[hsl(var(--surface-subtle))] rounded-[var(--radius-lg)] py-[var(--space-3)] px-[var(--space-2)]">
      <span className="caption-r text-[hsl(var(--text-secondary))]">{label}</span>
      <span className="h4-b text-[hsl(var(--text-primary))]">{value}</span>
      <span className="caption-r text-[hsl(var(--text-secondary))]">{unit}</span>
    </div>
  );
}

export function MonthlySummary({ runCount, distanceM, caloriesKcal }: MonthlySummaryProps) {
  // 소수점 둘째자리까지 표시 / 데이터 없으면 '--'
  const distanceKm = distanceM !== null ? (distanceM / 1000).toFixed(2) : '--';

  return (
    <div className="flex gap-[var(--space-3)]">
      <StatCard label="러닝 횟수" value={runCount !== null ? String(runCount) : '--'} unit="회" />
      <StatCard label="총 거리" value={distanceKm} unit="km" />
      <StatCard label="소모 칼로리" value={caloriesKcal !== null ? String(caloriesKcal) : '--'} unit="kcal" />
    </div>
  );
}
