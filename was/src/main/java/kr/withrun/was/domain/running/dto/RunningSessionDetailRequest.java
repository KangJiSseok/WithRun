package kr.withrun.was.domain.running.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "러닝 세션 상세 조회 요청")
public record RunningSessionDetailRequest(
        @NotNull
        @Positive
        @Schema(description = "상세 조회를 요청하는 사용자 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        Long userId
) {
}
