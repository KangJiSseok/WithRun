package kr.withrun.was.domain.course.dto;

import kr.withrun.was.domain.course.type.CourseStatus;

public record CourseLikeStatusResponse(
        Long courseId,
        boolean isLiked,
        long likeCount,
        CourseStatus status
) {
}
