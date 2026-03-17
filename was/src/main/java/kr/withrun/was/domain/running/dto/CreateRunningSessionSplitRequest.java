package kr.withrun.was.domain.running.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateRunningSessionSplitRequest(
        @NotNull
        @Positive
        Long userId,

        @NotNull
        @Positive
        Integer splitIndex,

        @NotNull
        @Positive
        Integer splitDistanceM,

        @NotNull
        @Positive
        Integer splitDurationSec,

        @NotNull
        @Positive
        Integer splitPaceSecPerKm,

        @Positive
        Integer avgHeartRate,

        @NotNull
        @PositiveOrZero
        Integer elevationGainM
) {
}
