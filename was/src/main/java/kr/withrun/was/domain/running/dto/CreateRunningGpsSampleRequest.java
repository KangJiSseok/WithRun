package kr.withrun.was.domain.running.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public record CreateRunningGpsSampleRequest (
        @NotNull
        Double latitude,

        @NotNull
        Double longitude,

        @NotNull
        Double altitudeM,

        @NotNull
        Double accuracyM,

        @NotNull
        Double bearingDeg,

        @NotNull
        LocalDateTime sampledAt
 ) {

}
