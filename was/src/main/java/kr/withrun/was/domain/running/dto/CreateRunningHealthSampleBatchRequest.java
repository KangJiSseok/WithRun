package kr.withrun.was.domain.running.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateRunningHealthSampleBatchRequest(
        @NotNull
        @Positive
        Long userId,

        @NotNull
        @Size(max = 120)
        List<@Valid CreateRunningHealthSampleRequest> samples
) {
}
