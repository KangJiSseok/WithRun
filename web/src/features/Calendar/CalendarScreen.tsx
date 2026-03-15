import { useCalendar } from '@/hooks/Calendar';
import { CalendarHeader } from './CalendarHeader';
import { CalendarGrid } from './CalendarGrid';
import { MonthlySummary } from './MonthlySummary';
import { DayDetail } from './DayDetail';

export function CalendarScreen() {
  const {
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
  } = useCalendar();

  return (
    <div className="flex flex-col gap-[var(--space-4)] h-full overflow-y-auto pb-[var(--space-6)]">
      {/* < 20xx년 x월 > */}
      <CalendarHeader
        year={year}
        month={month}
        isCurrentMonth={isCurrentMonth}
        onPrev={goPrev}
        onNext={goNext}
      />

      {/* 해당 월에 나오는 월별 요약표 (러닝 횟수, 총 거리, 소모 칼로리) */}
      {data && (
        <MonthlySummary
          runCount={data.monthlyRunCount}
          distanceM={data.monthlyDistanceM}
          caloriesKcal={data.monthlyCaloriesKcal}
        />
      )}

      {/* API 호출 중: 로딩 텍스트, 아니면 캘린더 그리드 표시 */}
      {/* API 응답 전에 CalendarGrid를 그리면 activeDates가 빈 Set */}
      {/* 빈 캘린더가 잠깐 보이는 현상 방지 */}
      {isLoading ? (
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
          // 날짜 클릭 핸들러 -> 클릭 시 selectedDate 업데이트
          onSelectDate={(date) => setSelectedDate(date)}
        />
      )}

      {/* 날짜 선택했고 기록 있음 */}
      {selectedDate && selectedSummary && (
        <DayDetail date={selectedDate} summary={selectedSummary} />
      )}

      {/* 로딩 했는데 기록 없음 */}
      {selectedDate && !selectedSummary && !isLoading && (
        <div className="rounded-[var(--radius-xl)] border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] p-[var(--space-4)] text-center">
          <p className="body-r text-[hsl(var(--text-secondary))]">이 날은 러닝 기록이 없습니다.</p>
        </div>
      )}
    </div>
  );
}
