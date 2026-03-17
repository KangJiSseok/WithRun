package kr.withrun.was.domain.running.dto;

import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.global.common.type.Difficulty;

public record RegisterRunningSessionCourseResponse(
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
        Coordinates coordinates
) {

    public static RegisterRunningSessionCourseResponse from(Course course, Difficulty difficulty) {
        return new RegisterRunningSessionCourseResponse(
                course.getId(),
                course.getTitle(),
                course.getStatus(),
                difficulty,
                course.getDistanceM(),
                course.getElevationGainM(),
                course.getSnapshotImageUrl(),
                course.getStartLatitude(),
                course.getStartLongitude(),
                course.getEndLatitude(),
                course.getEndLongitude(),
                course.getCoordinates()
        );
    }
}
