package kr.withrun.was.domain.running.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record CreateRunningGpsSampleRequest (
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

        Double altitudeM,

        Double accuracyM,

        Double bearingDeg,

        @NotNull
        LocalDateTime sampledAt
        ) {

}
