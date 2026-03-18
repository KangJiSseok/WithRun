package kr.withrun.was.domain.running.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.global.common.type.Difficulty;

@Schema(description = "러닝 기록을 기반으로 생성된 커뮤니티 코스 응답")
public record RegisterRunningSessionCourseResponse(
        @Schema(description = "생성된 코스 ID", example = "101")
        Long courseId,
        @Schema(description = "코스 제목", example = "한강 야간 러닝 10K")
        String title,
        @Schema(description = "코스 상태", example = "COMMUNITY")
        CourseStatus status,
        @Schema(description = "코스 난이도", example = "MEDIUM")
        Difficulty difficulty,
        @Schema(description = "코스 거리(m)", example = "10000")
        Integer distanceM,
        @Schema(description = "누적 상승 고도(m)", example = "120")
        Integer elevationGainM,
        @Schema(description = "코스 스냅샷 이미지 URL", example = "https://cdn.example.com/course/snapshot.png", nullable = true)
        String snapshotImageUrl,
        @Schema(description = "코스 시작 위도", example = "37.566501")
        Double startLatitude,
        @Schema(description = "코스 시작 경도", example = "126.978001")
        Double startLongitude,
        @Schema(description = "코스 종료 위도", example = "37.574501")
        Double endLatitude,
        @Schema(description = "코스 종료 경도", example = "126.989001")
        Double endLongitude,
        @Schema(description = "코스 전체 경로 좌표 목록")
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
