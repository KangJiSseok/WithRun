package kr.withrun.was.domain.running.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "건강 샘플 배치 저장 결과")
public record CreateRunningHealthSampleBatchResponse(
        @Schema(description = "샘플이 저장된 러닝 세션 ID", example = "91")
        Long sessionId,
        @Schema(description = "샘플을 적재한 사용자 ID", example = "7")
        Long userId,
        @Schema(description = "저장된 샘플 개수", example = "60")
        int savedCount
) {
    public static CreateRunningHealthSampleBatchResponse of(
            Long runningSessionId,
            Long userId,
            int savedCount
    ) {
        return new CreateRunningHealthSampleBatchResponse(runningSessionId, userId, savedCount);
    }
}
