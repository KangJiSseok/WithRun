import { ChevronDown } from 'lucide-react';
import { cn } from '@/shared/utils';
import { useCalendar } from '@/hooks/Calendar';
import { CalendarHeader } from './CalendarHeader';
import { CalendarGrid } from './CalendarGrid';
import { ActivitySummary } from './ActivitySummary';
import { RunningHistoryList } from './RunningHistoryList';
import { StreakCard } from './StreakCard';

// "2026-03-09" -> "3월 9일 활동"
function formatDayTitle(dateStr: string): string {
  const parts = dateStr.split('-');
  return `${Number(parts[1])}월 ${Number(parts[2])}일 활동`;
}

export function CalendarScreen() {
  const {
    year,
    month,
    monthlyData,
    isMonthlyLoading,
    summaryData,
    isSummaryLoading,
    historyData,
    isHistoryLoading,
    isScrollLoading,
    loadMore,
    selectedDate,
    toggleDate,
    activeDates,
    isCurrentMonth,
    goPrev,
    goNext,
    today,
    isCalendarOpen,
    setIsCalendarOpen,
  } = useCalendar();

  // 상단 요약 통계
  // - 달력 닫힘: "전체 요약" + lifetime 데이터
  // - 달력 열림: "월별 요약" + monthly 데이터
  const summaryLabel = isCalendarOpen ? '월별 요약' : '전체 요약';
  const summaryRunCount = isCalendarOpen
    ? (monthlyData?.monthlyRunCount ?? null)
    : (summaryData?.lifetimeRunCount ?? null);
  const summaryDistanceM = isCalendarOpen
    ? (monthlyData?.monthlyDistanceM ?? null)
    : (summaryData?.lifetimeDistanceM ?? null);

  // 러닝 기록 리스트 제목
  // - 날짜 선택됨: "3월 9일 활동" (달력 열릴 때만)
  // - 달력 열림 + 날짜 미선택: "2026년 3월 활동"
  // - 달력 닫힘 + 날짜 미선택: "전체 기록"
  const historyTitle = !isCalendarOpen
    ? '전체 기록'
    : selectedDate
      ? formatDayTitle(selectedDate)
      : `${year}년 ${month}월 활동`;

  return (
    <div className="flex flex-col gap-[var(--space-4)] h-full overflow-y-auto pb-[var(--space-6)]">
      {/* 화면 제목 */}
      <p className="h3-b text-[hsl(var(--text-primary))]">활동 캘린더</p>

      {/* 연속 러닝 기록 (스트릭) - summary API에서 받아온 데이터 */}
      <StreakCard
        currentStreakDays={summaryData?.currentStreakDays ?? null}
        longestStreakDays={summaryData?.longestStreakDays ?? null}
        isLoading={isSummaryLoading}
      />

      {/* 요약 통계 (달력 상태에 따라 전체/월별 전환) */}
      <ActivitySummary
        label={summaryLabel}
        runCount={summaryRunCount}
        distanceM={summaryDistanceM}
        isLoading={isSummaryLoading}
      />

      {/* 달력 보기/숨기기 토글 */}
      <button
        type="button"
        onClick={() => {
          // selectedDate 유지 -> 달력 다시 열면 이전 선택 날짜 & RunningHistoryList(일별) 복원
          setIsCalendarOpen((prev) => !prev);
        }}
        className="w-full flex justify-between items-center p-[var(--space-3)] rounded-[var(--radius-xl)] border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] text-[hsl(var(--text-primary))] hover:bg-[hsl(var(--surface-subtle))] transition-colors"
      >
        <span className="body-b">{isCalendarOpen ? '달력 숨기기' : '달력 보기'}</span>
        <ChevronDown
          size={20}
          className={cn('transition-transform duration-300', isCalendarOpen && 'rotate-180')}
        />
      </button>

      {/* 달력 - 둥근 박스에 담아 토글 */}
      <div
        className={cn(
          'grid transition-all duration-300 ease-in-out',
          isCalendarOpen ? 'grid-rows-[1fr] opacity-100' : 'grid-rows-[0fr] opacity-0',
        )}
      >
        <div className="overflow-hidden">
          <div className="rounded-[var(--radius-xl)] border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] p-[var(--space-4)] flex flex-col gap-[var(--space-3)]">
            {/* < 20xx년 x월 > */}
            <CalendarHeader
              year={year}
              month={month}
              isCurrentMonth={isCurrentMonth}
              onPrev={goPrev}
              onNext={goNext}
            />
            {/* API 호출 중: 로딩 텍스트, 아니면 캘린더 그리드 표시 */}
            {/* API 응답 전에 CalendarGrid를 그리면 activeDates가 빈 Set */}
            {/* 빈 캘린더가 잠깐 보이는 현상 방지 */}
            {isMonthlyLoading ? (
              <div className="flex justify-center py-8">
                <span className="body-r text-[hsl(var(--text-secondary))]">로딩 중...</span>
              </div>
            ) : (
              <CalendarGrid
                // 현재 보는 연(년)
                year={year}
                // 현재 보는 월
                month={month}
                // 운동한 날짜 Set (빨간 점 표시 여부)
                activeDates={activeDates}
                // 클릭한 날짜 or null값 (하이라이트 표시)
                selectedDate={selectedDate}
                // 오늘 날짜 표시 + 미래 날짜 비활성화
                today={today}
                // 날짜 클릭 핸들러 -> 같은 날짜 재클릭 시 선택 해제 → 월별 기록으로 복귀
                onSelectDate={toggleDate}
              />
            )}
          </div>
        </div>
      </div>

      {/* 러닝 기록 리스트 (항상 표시, 상태에 따라 전체/월별/일별 전환) */}
      <RunningHistoryList
        title={historyTitle}
        items={historyData?.items ?? []}
        isLoading={isHistoryLoading}
        // useCalendar에서 isScrollLoading, loadMore 받아
        // RunningHistoryList에 전달
        hasMore={historyData?.hasMore ?? false}
        isScrollLoading={isScrollLoading}
        onLoadMore={loadMore}
      />
    </div>
  );
}
