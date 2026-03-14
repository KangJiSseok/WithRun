package kr.withrun.was.domain.course.dto;

import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.global.common.type.Difficulty;

import java.time.LocalDateTime;
import java.util.List;

public record BookmarkedCourseItemResponse(
        Long bookmarkId,
        LocalDateTime bookmarkedAt,
        Long courseId,
        String title,
        CourseStatus status,
        Integer distanceM,
        Integer elevationGainM,
        Difficulty difficulty,
        List<CourseType> courseTypes,
        String snapshotImageUrl,
        boolean isBookmarked,
        boolean isRunnable
) {
}
