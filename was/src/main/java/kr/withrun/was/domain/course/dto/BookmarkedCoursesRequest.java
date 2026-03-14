package kr.withrun.was.domain.course.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record BookmarkedCoursesRequest(
        @Positive
        Long userId,

        @NotNull
        @Positive
        @Max(50)
        Integer size,

        String cursor
) {
}
