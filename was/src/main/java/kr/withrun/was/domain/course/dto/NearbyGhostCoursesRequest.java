package kr.withrun.was.domain.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

@Schema(description = "주변 고스트 코스 조회 조건")
public record NearbyGhostCoursesRequest(
        @Schema(description = "조회 중심 위도", example = "37.5665", minimum = "-90.0", maximum = "90.0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @DecimalMin(value = "-90.0")
        @DecimalMax(value = "90.0")
        Double latitude,

        @Schema(description = "조회 중심 경도", example = "126.9780", minimum = "-180.0", maximum = "180.0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @DecimalMin(value = "-180.0")
        @DecimalMax(value = "180.0")
        Double longitude,

        @Schema(description = "선호 거리 범위 목록", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty
        List<@Valid PreferredDistanceRange> preferredDistanceMs,

        @Schema(description = "한 번에 조회할 항목 수. 생략 시 기본값 3을 사용합니다.", example = "5", minimum = "1", maximum = "10", nullable = true)
        @Positive
        @Max(10)
        Integer pageSize,

        @Schema(description = "이전 응답의 nextCursor 값", example = "MzIwOjEy", nullable = true)
        String cursor
) {
}
