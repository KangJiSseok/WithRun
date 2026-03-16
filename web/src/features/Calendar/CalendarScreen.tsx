import { ChevronDown } from 'lucide-react';
import { cn } from '@/shared/utils';
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
    selectedSummaries,
    activeDates,
    isCurrentMonth,
    goPrev,
    goNext,
    today,
    showCalendar,
    setShowCalendar,
  } = useCalendar();

  return (
    <div className="flex flex-col gap-[var(--space-4)] h-full overflow-y-auto pb-[var(--space-6)]">
      {/* 해당 월에 나오는 월별 요약표 (러닝 횟수, 총 거리, 소모 칼로리) */}
      {/* 달력 보기/숨기기 여부와 상관없이 항상 표시 */}
      {/* API 응답 전이거나 실패 시 null 전달 -> 카드 틀은 유지, 값만 '--' */}
      <MonthlySummary
        runCount={data?.monthlyRunCount ?? null}
        distanceM={data?.monthlyDistanceM ?? null}
        caloriesKcal={data?.monthlyCaloriesKcal ?? null}
      />

      {/* 달력 보기/숨기기 토글 */}
      <button
        type="button"
        onClick={() => {
          // selectedDate 유지 -> 달력 다시 열면 이전 선택 날짜 & DayDetail 복원
          setShowCalendar((prev) => !prev);
        }}
        className="w-full flex justify-between items-center p-[var(--space-3)] rounded-[var(--radius-xl)] border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] text-[hsl(var(--text-primary))] hover:bg-[hsl(var(--surface-subtle))] transition-colors"
      >
        <span className="body-b">{showCalendar ? '달력 숨기기' : '달력 보기'}</span>
        <ChevronDown
          size={20}
          className={cn('transition-transform duration-300', showCalendar && 'rotate-180')}
        />
      </button>

      {/* 달력 - 둥근 박스에 담아 토글 */}
      <div
        className={cn(
          'grid transition-all duration-300 ease-in-out',
          showCalendar ? 'grid-rows-[1fr] opacity-100' : 'grid-rows-[0fr] opacity-0',
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
          </div>
        </div>
      </div>

      {/* 날짜 선택했고 기록 있음 - 날짜 헤더 1번 + 기록별 스탯 카드 */}
      {showCalendar && selectedDate && selectedSummaries.length > 0 && (
        <DayDetail date={selectedDate} summaries={selectedSummaries} />
      )}

      {/* 로딩 했는데 기록 없음 */}
      {showCalendar && selectedDate && selectedSummaries.length === 0 && !isLoading && (
        <div className="rounded-[var(--radius-xl)] border border-[hsl(var(--border-default))] bg-[hsl(var(--surface-default))] p-[var(--space-4)] text-center">
          <p className="body-r text-[hsl(var(--text-secondary))]">이 날은 러닝 기록이 없습니다.</p>
        </div>
      )}
    </div>
  );
}
