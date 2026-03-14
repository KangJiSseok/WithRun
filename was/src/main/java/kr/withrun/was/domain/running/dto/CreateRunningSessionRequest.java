package kr.withrun.was.domain.running.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import kr.withrun.was.domain.running.type.RunningMode;

import java.time.LocalDateTime;

public record CreateRunningSessionRequest(
        @NotNull
        @Positive
        Long userId,

        @NotNull
        RunningMode mode,

        @Positive
        Long courseId,

        @Positive
        Long ghostTargetRunningSessionId,

        @NotNull
        LocalDateTime startedAt,

        @NotNull
        @DecimalMin(value = "-90.0")
        @DecimalMax(value = "90.0")
        Double startLatitude,

        @NotNull
        @DecimalMin(value = "-180.0")
        @DecimalMax(value = "180.0")
        Double startLongitude
) {
}
