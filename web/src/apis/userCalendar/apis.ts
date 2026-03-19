import axiosClient from '@/shared/libs/axiosClient';
import type { ApiResponse } from '@/shared/types/api.type';
import type { CalendarSummaryData, MonthlyCalendarData } from './types';

// 회원 시스템 구현 전 임시 mock 유저
// 추후 변경 시 => auth context에서 실제 유저 정보 받아오기
const MOCK_USER_ID = 1;

/**
 * 전체 기간 누적 요약 + 스트릭 조회
 * GET /calendars/{userId}/summary
 */
export const getCalendarSummaryApi = async (): Promise<ApiResponse<CalendarSummaryData>> => {
  const { data } = await axiosClient.get<ApiResponse<CalendarSummaryData>>(
    `/calendars/${MOCK_USER_ID}/summary`,
  );
  return data;
};

/**
 * 월별 캘린더 데이터 조회
 * GET /calendars/{userId}?year=&month=
 */
export const getMonthlyCalendarApi = async (
  year: number,
  month: number,
): Promise<ApiResponse<MonthlyCalendarData>> => {
  const { data } = await axiosClient.get<ApiResponse<MonthlyCalendarData>>(
    `/calendars/${MOCK_USER_ID}`,
    { params: { year, month } },
  );
  return data;
};
