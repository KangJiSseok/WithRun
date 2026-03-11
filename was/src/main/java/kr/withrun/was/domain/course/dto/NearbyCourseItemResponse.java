package kr.withrun.was.domain.course.dto;

import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.global.common.type.Difficulty;

import java.util.List;

public record NearbyCourseItemResponse(
        Long courseId,
        String title,
        CourseStatus status,
        Integer distanceM,
        Integer elevationGainM,
        Double startLatitude,
        Double startLongitude,
        Double endLatitude,
        Double endLongitude,
        Integer distanceFromUserM,
        Difficulty difficulty,
        List<CourseType> courseTypes,
        String snapshotImageUrl
) {
}
