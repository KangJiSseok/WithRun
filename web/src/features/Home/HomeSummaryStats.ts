export type HomeSummaryStats = {
  runCount: number;
  distanceKm: number;
  caloriesKcal: number;
  durationMinutes: number;
};

// 백엔드 요약 API 가 생기기 전까지는 UI 계약만 먼저 고정하고 fixture 로 렌더링합니다.
const HOME_SUMMARY_STATS_FIXTURE: HomeSummaryStats = {
  runCount: 4,
  distanceKm: 23.5,
  caloriesKcal: 1842,
  durationMinutes: 135,
};

export function getHomeSummaryStats(): HomeSummaryStats {
  return HOME_SUMMARY_STATS_FIXTURE;
}
