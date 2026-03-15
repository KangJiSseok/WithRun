package kr.withrun.was.domain.user.dto;

import java.time.LocalDate;

public record UserCalendarSummaryResponse(
        Integer lifetimeRunCount,
        Integer lifetimeDistanceM,
        Integer lifetimeCourseRunCount,
        Integer lifetimeFreeRunCount,
        Integer lifetimeGhostRunCount,
        Integer currentStreakDays,
        Integer longestStreakDays,
        LocalDate calculatedDate
) {
}
