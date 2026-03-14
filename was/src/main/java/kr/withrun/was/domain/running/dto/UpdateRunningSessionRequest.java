package kr.withrun.was.domain.running.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.LocalDateTime;

public record UpdateRunningSessionRequest(
        @NotNull
        @Positive
        Long userId,

        @NotNull
        LocalDateTime endedAt,

        @NotNull
        @PositiveOrZero
        Integer distanceM,

        @NotNull
        @PositiveOrZero
        Integer caloriesKcal,

        @NotNull
        @DecimalMin(value = "-90.0")
        @DecimalMax(value = "90.0")
        Double endLatitude,

        @NotNull
        @DecimalMin(value = "-180.0")
        @DecimalMax(value = "180.0")
        Double endLongitude,

        @NotNull
        @PositiveOrZero
        Double avgSpeedMps,

        @NotNull
        @Positive
        Integer durationSec,

        @NotNull
        @Positive
        Integer avgPaceSecPerKm,

        @NotNull
        @PositiveOrZero
        Integer elevationGainM
) {
}
