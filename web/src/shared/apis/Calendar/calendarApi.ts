import axiosClient from '@/shared/libs/axiosClient';

// 회원 시스템 구현 전 임시 mock 유저
// 추후 변경 시 => auth context에서 실제 유저 정보 받아오기
const MOCK_USER_ID = 4;

// 활동이 있는 날짜만 받음
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

export async function fetchCalendar(year: number, month: number): Promise<CalendarData> {
  const res = await axiosClient.get<{ data: CalendarData }>(
    `/calendars/${MOCK_USER_ID}`,
    // URL 쿼리스트링 변환
    // /api/calendars/1?year=2026&month=3 이런식으로 GET 요청 보내기
    { params: { year, month } },
  );
  return res.data.data;
}
