package kr.withrun.was.domain.reward.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "리워드 draw pool 응답")
public record RewardDrawPoolResponse(
        @Schema(description = "리워드 draw pool ID", example = "1")
        Long rewardDrawPoolId,

        @Schema(description = "pool 이름", example = "런칭 풀")
        String name,

        @Schema(description = "현재 활성 여부", example = "true")
        Boolean isActive,

        @Schema(description = "pool item 목록")
        List<RewardDrawPoolItemResponse> items,

        @Schema(description = "생성 시각")
        LocalDateTime createdAt,

        @Schema(description = "수정 시각")
        LocalDateTime updatedAt
) {
}
