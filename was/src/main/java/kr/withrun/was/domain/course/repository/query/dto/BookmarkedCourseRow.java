package kr.withrun.was.domain.course.repository.query.dto;

import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.global.common.type.Difficulty;

import java.time.LocalDateTime;

public record BookmarkedCourseRow(
        Long bookmarkId,
        LocalDateTime bookmarkedAt,
        Long courseId,
        String title,
        CourseStatus status,
        Integer distanceM,
        Integer elevationGainM,
        Difficulty difficulty,
        CourseType courseType,
        String snapshotImageUrl,
        Boolean isBookmarked
) {
}
