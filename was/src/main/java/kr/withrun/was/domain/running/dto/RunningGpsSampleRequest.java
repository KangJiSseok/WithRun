package kr.withrun.was.domain.running.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jdk.jfr.Description;

import java.time.LocalDateTime;


public record RunningGpsSampleRequest(
        @NotNull
        @Positive
        Long id,

        @NotNull
        @Positive
        Long userId,

        @NotNull
        @Positive
        Long runningSessionId,

        @NotNull
        Double latitude,

        @NotNull
        Double longitude,

        @NotNull
        Double altitudeM,

        @NotNull
        Double accuracyM,

        @NotNull
        LocalDateTime sampledAt
) {
}
