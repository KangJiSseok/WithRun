package kr.withrun.was.domain.course.dto;

import java.util.List;

public record CourseLeaderboardResponse(
        Long courseId,
        List<CourseLeaderboardItemResponse> items,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {
}
