import { useEffect, useState } from 'react';
import { fetchCalendar, type CalendarData, type DailySummary } from '@/shared/apis/Calendar';

export function useCalendar() {
  // 날짜 표시 방법
  // 1. 렌더링마다 새 Date 객체 생성 (현재 방식)
  // 캘린더 -> 홈 이동 -> (자정 경과) -> 캘린더 재진입 ->
  // CalenderScreen 언마운트 -> 리마운트 -> useCalender 훅 처음부터 실행
  // new Date() = 자정 이후 날짜
  // 2. state + useEffect() => 화면 켜둔채로 있어도 setTimeout으로 갱신
  // (발생 가능성이 낮고, 오버엔지니어링)
  const today = new Date();
  const [year, setYear] = useState(today.getFullYear());
  // 0 ~ 11
  const [month, setMonth] = useState(today.getMonth() + 1);
  // 데이터: API응답 오기 전까지는 없는 상태
  // 1. 컴포넌트 마운트 -> data = null (API 응답 대기 중)
  // 2. fetchCalender 호출 완료 -> setData (응답) -> data = CalendarData
  // 3. 월 이동 시 -> 새 API 호출 -> 응답 오면 setData (새 데이터)
  const [data, setData] = useState<CalendarData | null>(null);
  // API 호출 중인지 아닌지를 나타내는 플래그
  // 초기값 false = 아직 로딩 중 아님
  const [isLoading, setIsLoading] = useState(false);
  // 선택한 날짜 (요일 별 운동 여부 및 내용 확인 가능)
  const [selectedDate, setSelectedDate] = useState<string | null>(null);

  const isCurrentMonth =
    year === today.getFullYear() && month === today.getMonth() + 1;

  // 마운트 시 1번 + year 또는 month 바뀔 때마다 effect 다시 실행
  useEffect(() => {
    setIsLoading(true);
    fetchCalendar(year, month)
      .then(setData)
      .catch(() => setData(null))
      // 성공, 실패 상관없이 로딩 끝 처리
      .finally(() => setIsLoading(false));
  }, [year, month]);

  function goPrev() {
    if (month === 1) {
      setYear((y) => y - 1);
      setMonth(12);
    } else {
      setMonth((m) => m - 1);
    }
    setSelectedDate(null);
  }

  // 미래 버튼 막기
  function goNext() {
    if (isCurrentMonth) return;
    if (month === 12) {
      setYear((y) => y + 1);
      setMonth(1);
    } else {
      setMonth((m) => m + 1);
    }
    setSelectedDate(null);
  }

  // 운동 기록이 있는 날짜 조회하기 위함 (운동한 날짜에 빨간 점이 찍힘)
  const activeDates = new Set(
    data?.dailySummaries.map((s) => s.calendarDate) ?? [],
  );

  // selectedSummary
  // 1. 날짜 미선택: null
  // 2. 날짜 선택 했는데, 기록이 없음: null
  // 3. 날짜 선택 + 기록 있음 (DailySummary 객체)
  const selectedSummary: DailySummary | null = selectedDate
    ? (data?.dailySummaries.find((s) => s.calendarDate === selectedDate) ?? null)
    : null;

  return {
    year,
    month,
    data,
    isLoading,
    selectedDate,
    setSelectedDate,
    selectedSummary,
    activeDates,
    isCurrentMonth,
    goPrev,
    goNext,
    today,
  };
}
