package kr.withrun.was.domain.running.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "건강 샘플 배치 저장 요청")
public record CreateRunningHealthSampleBatchRequest(
        @NotNull
        @Positive
        @Schema(description = "샘플을 적재하는 사용자 ID", example = "7", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Long userId,

        @NotNull
        @Size(max = 120)
        @Schema(description = "최대 120건의 건강 샘플 목록", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@Valid CreateRunningHealthSampleRequest> samples
) {
}
