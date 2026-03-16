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
  // 달력 그리드 표시 여부 (토글 버튼으로 제어)
  const [showCalendar, setShowCalendar] = useState(false);

  const isCurrentMonth =
    year === today.getFullYear() && month === today.getMonth() + 1;

  // 마운트 시 1번 + year 또는 month 바뀔 때마다 effect 다시 실행

  // useEffect의 콜백은 cleanup 함수 또는 undefined 반환해야 하는데,
  // async 함수는 Promise를 항상 반환하므로, useEffect 콜백 자체를 async로 만들 수는 없음.
  // (useEffect가 Promise를 cleanup 함수로 오해할 수 있기 때문)
  // 따라서 내부의 별도 async 함수를 선언하고 즉시 호출
  
  // 이전 방식: Promise 체이닝 (effect 본문에서 동기 setState 호출)
  // => effect 콜백 본문에서 동기적으로 setIsLoading(true) 호출 -> React가
  // 렌더링 중에 상태 변경을 감지해 cascading render 경고 발생할 수 있음
  // 현재 방식: async 함수 내부로 이동 -> 비동기 컨텍스트에서 호출되므로 경고 방지
  useEffect(() => {
    // setIsLoading(true)를 async 함수 안으로 이동
    // -> effect 본문에서 동기 setState 호출 시 캐스케이딩 렌더 경고 방지
    async function load() {
      setIsLoading(true);
      try {
        const result = await fetchCalendar(year, month);
        setData(result);
      } catch {
        setData(null);
      } finally {
        setIsLoading(false);
      }
    }
    // 호출만 하고, await는 하지 않음
    // => Promise를 useEffect에 반환하지 않음
    load();
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

  // selectedSummaries
  // 1. 날짜 미선택: []
  // 2. 날짜 선택 했는데 기록 없음: []
  // 3. 날짜 선택 + 기록 있음: 해당 날짜의 모든 DailySummary 배열 (같은 날 여러 운동 가능)
  // null -> [] 변경 이유
  // 1. 미선택과 선택했으나 기록이 없음을 구분
  // selectedDate === null => selectedSummaries는 [] (날짜 선택 안 했으니 기록 없음)
  // selectedSummaries.length === 0 => 날짜 선택했는데 기록 없음
  // 2. .map() 호출 안정성 (null이면 selectedSummaries.map() 하면 런타임 에러 발생)
  const selectedSummaries: DailySummary[] = selectedDate
    ? (data?.dailySummaries.filter((s) => s.calendarDate === selectedDate) ?? [])
    : [];

  return {
    year,
    month,
    data,
    isLoading,
    selectedDate,
    setSelectedDate,
    selectedSummaries,
    activeDates,
    isCurrentMonth,
    goPrev,
    goNext,
    today,
    showCalendar,
    setShowCalendar,
  };
}
