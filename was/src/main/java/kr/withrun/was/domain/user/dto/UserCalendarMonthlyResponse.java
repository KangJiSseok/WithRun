package kr.withrun.was.domain.user.dto;

import kr.withrun.was.domain.user.repository.query.dto.UserCalendarDailySummaryRow;

import java.time.LocalDate;
import java.util.List;

public record UserCalendarMonthlyResponse(
        Integer year,
        Integer month,
        Integer monthlyRunCount,
        Integer monthlyDistanceM,
        Integer monthlyCaloriesKcal,
        Integer monthlyCourseRunCount,
        Integer monthlyFreeRunCount,
        Integer monthlyGhostRunCount,
        List<DailySummary> dailySummaries
) {
    public static UserCalendarMonthlyResponse from(Integer year, Integer month, List<DailySummary> dailySummaries) {
        return new UserCalendarMonthlyResponse(
                year,
                month,
                dailySummaries.stream()
                        .mapToInt(DailySummary::dailyRunCount)
                        .sum(),
                dailySummaries.stream()
                        .mapToInt(DailySummary::dailyDistanceM)
                        .sum(),
                dailySummaries.stream()
                        .mapToInt(DailySummary::dailyCaloriesKcal)
                        .sum(),
                dailySummaries.stream()
                        .mapToInt(DailySummary::dailyCourseRunCount)
                        .sum(),
                dailySummaries.stream()
                        .mapToInt(DailySummary::dailyFreeRunCount)
                        .sum(),
                dailySummaries.stream()
                        .mapToInt(DailySummary::dailyGhostRunCount)
                        .sum(),
                dailySummaries
        );
    }

    public record DailySummary(
            LocalDate calendarDate,
            Integer dailyDistanceM,
            Integer dailyDurationSec,
            Integer dailyCaloriesKcal,
            Integer dailyCourseRunCount,
            Integer dailyFreeRunCount,
            Integer dailyGhostRunCount
    ) {
        public static DailySummary from(UserCalendarDailySummaryRow dailySummaryRow) {
            return new DailySummary(
                    dailySummaryRow.calendarDate(),
                    dailySummaryRow.dailyDistanceM(),
                    dailySummaryRow.dailyDurationSec(),
                    dailySummaryRow.dailyCaloriesKcal(),
                    dailySummaryRow.dailyCourseRunCount(),
                    dailySummaryRow.dailyFreeRunCount(),
                    dailySummaryRow.dailyGhostRunCount()
            );
        }

        public int dailyRunCount() {
            return dailyCourseRunCount + dailyFreeRunCount + dailyGhostRunCount;
        }
    }
}
