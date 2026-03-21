package kr.withrun.was.domain.course.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record NearbyGhostCoursesResponse(
        @ArraySchema(schema = @Schema(implementation = NearbyCourseItemResponse.class), arraySchema = @Schema(description = "현재 페이지 주변 코스 목록"))
        List<NearbyCourseItemResponse> items,

        @Schema(description = "현재 조건에서 다음 페이지가 더 있는지 여부", example = "true")
        boolean hasMore,

        @Schema(description = "다음 페이지 조회용 불투명 커서. hasMore 가 true 일 때만 제공된다.", example = "MzIwOjEy", nullable = true)
        String nextCursor
) {
}
