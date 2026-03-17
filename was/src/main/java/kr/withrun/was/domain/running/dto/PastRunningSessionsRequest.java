package kr.withrun.was.domain.running.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.DateTimeException;
import java.time.LocalDate;

public record PastRunningSessionsRequest(

        @NotNull
        @Positive
        Long userId,

        @Positive
        @Max(999999999)
        Integer year,

        @Min(1)
        @Max(12)
        Integer month,

        @Min(1)
        @Max(31)
        Integer day,

        @Positive
        Integer pageSize,

        String cursor
) {

    public PastRunningSessionsRequest {
        pageSize = pageSize == null ? 10 : pageSize;
    }

    //날짜 조합이 올바른지
    @AssertTrue
    public boolean isDateFilterCombinationValid() {

        // month는 year 없이 올 수 없음
        if (month != null && year == null) {
            return false;
        }

        // day는 year와 month가 모두 있어야 함
        if (day != null && (year == null || month == null)) {
            return false;
        }

        return true;
    }

    //유효한 날짜인지
    @AssertTrue
    public boolean isCalendarDateValid() {
        if (!isDateFilterCombinationValid()) {
            return true;
        }
        if (year == null) {
            return true;
        }

        try {
            LocalDate.of(
                    year,
                    month == null ? 1 : month,
                    day == null ? 1 : day
            );
            return true;
        } catch (DateTimeException e) {
            return false;
        }
    }
}
