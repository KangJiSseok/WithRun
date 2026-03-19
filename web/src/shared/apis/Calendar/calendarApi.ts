import axiosClient from '@/shared/libs/axiosClient';

// 회원 시스템 구현 전 임시 mock 유저
// 추후 변경 시 => auth context에서 실제 유저 정보 받아오기
const MOCK_USER_ID = 1;

// 전체 기간 누적 기록 보여주기
// 1. 홈에서 캘린더 화면을 처음 진입할 때 (달력 닫힌 상태)
// 2. 캘린더 화면에서 달력을 닫았을 때 (달력 열림 → 닫힘)
// 이 API 호출은 /running-sessions/history/past API와 함께 전체 기록 조회하는 용도로 사용
// GET /calendars/{userId}/summary - 전체 기간 누적 요약 + 스트릭
export interface CalendarSummaryData {
  lifetimeRunCount: number;
  lifetimeDistanceM: number;
  lifetimeCourseRunCount: number;
  lifetimeFreeRunCount: number;
  lifetimeGhostRunCount: number;
  currentStreakDays: number;
  longestStreakDays: number;
  calculatedDate: string;
}

export async function fetchCalendarSummary(): Promise<CalendarSummaryData> {
  const res = await axiosClient.get<{ data: CalendarSummaryData }>(
    `/calendars/${MOCK_USER_ID}/summary`,
  );
  return res.data.data;
}

// GET /running-sessions/history/past?userId={userId} - 과거 러닝 세션 조회
// 이 API는 전체 기록 조회(달력 닫힘) + 월별/일별 기록 조회(달력 열림) 모두에 사용
// 1. 전체 기록 조회 시 params 없이 호출 → 전체 기간 기록 반환
// 2. 월별 기록 조회 시 year, month 전달 → 해당 월 기록 반환
// 3. 일별 기록 조회 시 year, month, day 전달 → 해당 날짜 기록 반환
export interface RunningSession {
  runningSessionId: number;
  startedAt: string;
  distanceM: number;
  durationSec: number | null;
  caloriesKcal: number;
  snapshotImageUrl: string | null;
  elevationGainM: number;
  isCompleted: boolean;
  runningMode: string;
  ghostResultStatus: string | null;
  timeSlot: string;
}

export interface RunningHistoryData {
  items: RunningSession[];
  hasMore: boolean;
  nextCursor: string | null;
}

export interface RunningHistoryParams {
  // undefined 허용: API 호출 시점에 달력에서 선택된 날짜가 없을 수도 있기 때문
  // axios가 알아서 쿼리스트링에서 제외해줌
  year?: number;
  month?: number;
  day?: number;
  // 다음 페이지 시작점
  cursor?: string;
  // 한 번에 가져올 개수
  pageSize?: number;
}

export async function fetchRunningHistory(
  params: RunningHistoryParams = {},
): Promise<RunningHistoryData> {
  const res = await axiosClient.get<{ data: RunningHistoryData }>(
    `/running-sessions/history/past`,
    // params 객체를 GET 요청의 쿼리 파라미터로 전달 (예: ?userId=1&year=2026&month=3&day=16)
    { params: { userId: MOCK_USER_ID, ...params } },
  );
  return res.data.data;
}

// 달력 그리드에서 날짜별 운동 기록 표시에 사용
// 1. 달력을 열었을 때 해당 월의 날짜별 요약 데이터를 받아 운동한 날짜에 빨간 점 표시
// GET /calendars/{userId}?year=2026&month=3 응답의 dailySummaries 배열 항목
// 활동이 있는 날짜만 포함됨 (운동 기록 없는 날짜는 배열에 포함되지 않음)
export interface DailySummary {
  calendarDate: string;
  dailyDistanceM: number;
  dailyDurationSec: number;
  dailyCaloriesKcal: number;
  dailyCourseRunCount: number;
  dailyFreeRunCount: number;
  dailyGhostRunCount: number;
}

export interface CalendarData {
  year: number;
  month: number;
  monthlyRunCount: number;
  monthlyDistanceM: number;
  monthlyCaloriesKcal: number;
  monthlyCourseRunCount: number;
  monthlyFreeRunCount: number;
  monthlyGhostRunCount: number;
  dailySummaries: DailySummary[];
}

// 달력을 열거나 월을 이동할 때 호출
// GET /calendars/{userId}?year=2026&month=3
// 응답의 monthlyRunCount, monthlyDistanceM → ActivitySummary 월별 통계 표시
// 응답의 dailySummaries → 달력 그리드 빨간 점 (날짜 클릭 시 집계 데이터는 없는게 좋을 것 같음)
export async function fetchCalendar(year: number, month: number): Promise<CalendarData> {
  const res = await axiosClient.get<{ data: CalendarData }>(
    `/calendars/${MOCK_USER_ID}`,
    // /calendars/1?year=2026&month=3 형태로 GET 요청
    { params: { year, month } },
  );
  return res.data.data;
}
