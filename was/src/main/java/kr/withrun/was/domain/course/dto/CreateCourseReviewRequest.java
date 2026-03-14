package kr.withrun.was.domain.course.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record CreateCourseReviewRequest(
        @NotNull
        @Positive
        Long userId,

        @NotNull
        @Min(1)
        @Max(5)
        Integer rating,

        @NotEmpty
        List<@NotBlank String> courseTypes,

        @NotBlank
        String submittedDifficulty
) {
}
