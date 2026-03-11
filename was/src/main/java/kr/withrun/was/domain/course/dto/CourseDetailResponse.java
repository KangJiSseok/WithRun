package kr.withrun.was.domain.course.dto;

import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.global.common.type.Difficulty;

import java.util.List;

public record CourseDetailResponse(
        Long courseId,
        String title,
        CourseStatus status,
        Difficulty difficulty,
        Integer distanceM,
        Integer elevationGainM,
        String snapshotImageUrl,
        Double startLatitude,
        Double startLongitude,
        Double endLatitude,
        Double endLongitude,
        Coordinates coordinates,
        List<CourseType> types,
        Long likeCount,
        boolean isLiked,
        boolean isBookmarked,
        Double averageRating
) {
}
