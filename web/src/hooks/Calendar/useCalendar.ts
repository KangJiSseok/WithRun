import { useEffect, useRef, useState } from 'react';
import {
  // CalendarData
  fetchCalendar,
  // CalendarSummaryData
  fetchCalendarSummary,
  // RunningHistoryData
  fetchRunningHistory,
  type CalendarData,
  type CalendarSummaryData,
  type RunningHistoryData,
} from '@/shared/apis/Calendar';

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
  // 월별 캘린더 데이터 (달력 그리드용)
  // 1. 컴포넌트 마운트 -> data = null, 달력이 닫혀있으므로 API 호출 안 함
  // 2. 달력 열릴 때 -> fetchCalendar 호출 -> 응답 오면 data = CalendarData
  // 3. 월 이동 시 -> 새 API 호출 -> 응답 오면 setData (새 데이터)
  const [data, setData] = useState<CalendarData | null>(null);
  // API 호출 중인지 아닌지를 나타내는 플래그
  // 초기값 false = 아직 로딩 중 아님
  const [isLoading, setIsLoading] = useState(false);
  // 전체 기간 요약 (달력 닫힌 상태에서 ActivitySummary에 표시)
  const [summaryData, setSummaryData] = useState<CalendarSummaryData | null>(null);
  // 러닝 기록 리스트 (항상 표시, 상태에 따라 전체/월별/일별 전환)
  const [historyData, setHistoryData] = useState<RunningHistoryData | null>(null);
  const [isHistoryLoading, setIsHistoryLoading] = useState(false);
  // 선택한 날짜 (달력에서 클릭, 재클릭 시 해제)
  const [selectedDate, setSelectedDate] = useState<string | null>(null);
  // 달력 그리드 표시 여부 (토글 버튼으로 제어)
  const [showCalendar, setShowCalendar] = useState(false);

  const isCurrentMonth =
    year === today.getFullYear() && month === today.getMonth() + 1;

  // useEffect의 콜백은 cleanup 함수 또는 undefined 반환해야 하는데,
  // async 함수는 Promise를 항상 반환하므로, useEffect 콜백 자체를 async로 만들 수는 없음.
  // (useEffect가 Promise를 cleanup 함수로 오해할 수 있기 때문)
  // 따라서 내부의 별도 async 함수를 선언하고 즉시 호출

  // 이전 방식: Promise 체이닝 (effect 본문에서 동기 setState 호출)
  // => effect 콜백 본문에서 동기적으로 setIsLoading(true) 호출 -> React가
  // 렌더링 중에 상태 변경을 감지해 cascading render 경고 발생할 수 있음
  // 현재 방식: async 함수 내부로 이동 -> 비동기 컨텍스트에서 호출되므로 경고 방지

  // 마운트 시 1회: 전체 기간 요약 + 전체 러닝 기록 조회
  // 컴포넌트 마운트 -> fetchCalendarSummary() 호출
  // -> 성공: setSummaryData(응답 데이터)
  // -> 실패: setSummaryData(null) (에러 메시지 대신 빈 화면)
  // --> setIsHistoryLoading(true) -> 로딩 스피너 표시
  // -> fetchRunningHistory({}) 호출 (매개변수 없이 전체 기록 조회)
  // -> 성공: setHistoryData(응답 데이터)
  // -> 실패: setHistoryData(null)
  // --> finally: setIsHistoryLoading(false) -> 로딩 스피너 숨김
  useEffect(() => {
    let cancelled = false;
    async function loadInitial() {
      try {
        const summary = await fetchCalendarSummary();
        if (!cancelled) setSummaryData(summary);
      } catch {
        if (!cancelled) setSummaryData(null);
      }
      if (cancelled) return;
      setIsHistoryLoading(true);
      try {
        const history = await fetchRunningHistory({});
        if (!cancelled) setHistoryData(history);
      } catch {
        if (!cancelled) setHistoryData(null);
      } finally {
        if (!cancelled) setIsHistoryLoading(false);
      }
    }
    loadInitial();
    return () => { cancelled = true; };
  }, []);

  // 달력이 닫힐 때 (true → false): summary + 전체 history 재조회
  // [showCalendar] 의존성 배열 (달력 닫힐 때 재조회)
  const prevShowCalendarRef = useRef<boolean | null>(null);
  useEffect(() => {
    const wasOpen = prevShowCalendarRef.current === true;
    prevShowCalendarRef.current = showCalendar;
    // 이전 값이 true(달력 열려있었음) && 현재 값이 false(달력 닫힘)
    if (!wasOpen || showCalendar) return;
    let cancelled = false;
    // 성공: summaryData에 새 데이터, 실패: summaryData = null
    fetchCalendarSummary()
      .then((data) => { if (!cancelled) setSummaryData(data); })
      .catch(() => { if (!cancelled) setSummaryData(null); });
    // 달력 닫히면 전체 기록으로 복귀 → params 없이 호출
    setIsHistoryLoading(true);
    fetchRunningHistory({})
      .then((data) => { if (!cancelled) setHistoryData(data); })
      .catch(() => { if (!cancelled) setHistoryData(null); })
      .finally(() => { if (!cancelled) setIsHistoryLoading(false); });
    return () => { cancelled = true; };
  }, [showCalendar]);

  // 달력이 열려있을 때 year/month 변경 시 월별 캘린더 데이터 조회
  // 달력이 닫혀있으면 skip (데이터 불필요)
  // [showCalendar, year, month] 의존성 배열 (달력 열릴 때 + 월 이동 시 재조회)
  useEffect(() => {
    if (!showCalendar) return;
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
    load();
  }, [showCalendar, year, month]);

  // 러닝 기록: 달력이 열릴 때 / year·month·selectedDate 변경 시 재조회
  // 달력이 닫히면 skip → 닫힐 때의 전체 기록 재조회는 위 effect에서 처리
  // [showCalendar, year, month, selectedDate] 의존성 배열
  useEffect(() => {
    // 달력 닫혀있으면 skip (닫힐 때 전체 기록 재조회는 위 effect에서 처리)
    if (!showCalendar) return;
    let cancelled = false;
    async function loadHistory() {
      setIsHistoryLoading(true);
      try {
        // 파라미터 결정 (selectedDate 있으면 일별, 없으면 월별)
        let params: { year?: number; month?: number; day?: number } = {};
        if (selectedDate) {
          // "2026-03-16" -> { year: 2026, month: 3, day: 16 }
          const parts = selectedDate.split('-');
          params = {
            year: Number(parts[0]),
            month: Number(parts[1]),
            day: Number(parts[2]),
          };
        } else {
          params = { year, month };
        }
        const history = await fetchRunningHistory(params);
        // 이전 요청보다 늦게 도착한 응답은 무시 (race condition 방지)
        if (!cancelled) setHistoryData(history);
      } catch {
        if (!cancelled) setHistoryData(null);
      } finally {
        if (!cancelled) setIsHistoryLoading(false);
      }
    }
    loadHistory();
    return () => { cancelled = true; };
  }, [showCalendar, year, month, selectedDate]);

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

  // 날짜 토글: 같은 날짜 재클릭 시 선택 해제 → 월별 기록으로 복귀
  function toggleDate(date: string) {
    setSelectedDate((prev) => (prev === date ? null : date));
  }

  return {
    year,
    month,
    data,
    isLoading,
    summaryData,
    historyData,
    isHistoryLoading,
    selectedDate,
    toggleDate,
    activeDates,
    isCurrentMonth,
    goPrev,
    goNext,
    today,
    showCalendar,
    setShowCalendar,
  };
}
