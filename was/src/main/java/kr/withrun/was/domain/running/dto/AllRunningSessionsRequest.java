package kr.withrun.was.domain.running.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AllRunningSessionsRequest(
        @Positive
        Long userId,

        @NotNull
        @Positive
        @Max(50)
        Integer pageSize,

        String cursor
) {
}
