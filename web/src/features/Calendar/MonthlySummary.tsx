// 월별 통계 요약
// 러닝 횟수, 총 거리 (km), 소모 칼로리
interface MonthlySummaryProps {
  runCount: number;
  distanceM: number;
  caloriesKcal: number;
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
  // 소수점 둘째자리까지 표시
  const distanceKm = (distanceM / 1000).toFixed(2);

  return (
    <div className="flex gap-[var(--space-3)]">
      <StatCard label="러닝 횟수" value={String(runCount)} unit="회" />
      <StatCard label="총 거리" value={distanceKm} unit="km" />
      <StatCard label="소모 칼로리" value={String(caloriesKcal)} unit="kcal" />
    </div>
  );
}
