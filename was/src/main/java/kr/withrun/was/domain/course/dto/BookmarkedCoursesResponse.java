package kr.withrun.was.domain.course.dto;

import java.util.List;

public record BookmarkedCoursesResponse(
        List<BookmarkedCourseItemResponse> items,
        boolean hasMore,
        String nextCursor
) {
}
