package kr.withrun.was.domain.running.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateRunningGpsSampleBatchRequest(
        @NotNull
        @Positive
        Long userId,

        @NotEmpty
        @Size(max = 120)
        List<@Valid CreateRunningGpsSampleRequest> samples
) {
}
