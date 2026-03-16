package kr.withrun.was.domain.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UserCalendarMonthlyRequest(
        @NotNull
        @Positive
        @Max(999999999)
        Integer year,

        @NotNull
        @Min(1)
        @Max(12)
        Integer month
) {
}
