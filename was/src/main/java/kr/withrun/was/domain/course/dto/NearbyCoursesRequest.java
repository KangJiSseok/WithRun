package kr.withrun.was.domain.course.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record NearbyCoursesRequest(
        @NotNull
        @DecimalMin(value = "-90.0")
        @DecimalMax(value = "90.0")
        Double latitude,

        @NotNull
        @DecimalMin(value = "-180.0")
        @DecimalMax(value = "180.0")
        Double longitude,

        @NotNull
        @Min(1000)
        @Max(5000)
        Integer radiusM,

        @NotNull
        @Positive
        Integer targetDistanceM,

        String cursor
) {
}
