package kr.withrun.was.domain.course.dto;

import java.util.List;

public record NearbyCoursesResponse(
        List<NearbyCourseItemResponse> items,
        boolean hasMore,
        String nextCursor,
        Integer suggestedNextRadiusM
) {
}
