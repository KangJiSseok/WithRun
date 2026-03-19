package kr.withrun.was.domain.running.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RunningSessionDetailRequest(
        @NotNull @Positive Long userId
) {
}
