package kr.withrun.was.domain.course.dto;

import java.time.LocalDateTime;

public record CourseBookmarkAddResponse(
        Long courseId,
        boolean isBookmarked,
        LocalDateTime createdAt
) {
}
