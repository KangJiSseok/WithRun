package kr.withrun.was.domain.course.dto;

public record CourseBookmarkRemoveResponse(
        Long courseId,
        boolean isBookmarked
) {
}
