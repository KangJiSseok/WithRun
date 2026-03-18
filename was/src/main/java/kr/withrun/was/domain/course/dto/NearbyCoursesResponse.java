package kr.withrun.was.domain.course.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "주변 코스 조회 결과")
public record NearbyCoursesResponse(
        @ArraySchema(schema = @Schema(implementation = NearbyCourseItemResponse.class), arraySchema = @Schema(description = "현재 페이지 주변 코스 목록"))
        List<NearbyCourseItemResponse> items,

        @Schema(description = "현재 조건에서 다음 페이지가 더 있는지 여부", example = "true")
        boolean hasMore,

        @Schema(description = "다음 페이지 조회용 불투명 커서. hasMore 가 true 일 때만 제공된다.", example = "MzIwOjEy", nullable = true)
        String nextCursor,

        @Schema(description = "더 볼 코스가 없고 최대 반경에 도달하지 않았을 때 추천하는 다음 탐색 반경(m)", example = "4000", nullable = true)
        Integer suggestedNextRadiusM
) {
}
