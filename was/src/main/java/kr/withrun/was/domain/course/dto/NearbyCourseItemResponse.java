package kr.withrun.was.domain.course.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.global.common.type.Difficulty;

import java.util.List;

@Schema(description = "주변 코스 단건 정보")
public record NearbyCourseItemResponse(
        @Schema(description = "코스 ID", example = "12")
        Long courseId,

        @Schema(description = "코스 제목", example = "한강 야간 러닝 5K")
        String title,

        @Schema(description = "코스 상태")
        CourseStatus status,

        @Schema(description = "코스 총 거리(m)", example = "5200")
        Integer distanceM,

        @Schema(description = "코스 총 누적 상승고도(m)", example = "48")
        Integer elevationGainM,

        @Schema(description = "코스 시작 지점 위도", example = "37.5661")
        Double startLatitude,

        @Schema(description = "코스 시작 지점 경도", example = "126.9738")
        Double startLongitude,

        @Schema(description = "코스 종료 지점 위도", example = "37.5702")
        Double endLatitude,

        @Schema(description = "코스 종료 지점 경도", example = "126.9814")
        Double endLongitude,

        @Schema(description = "사용자 현재 위치와 코스 사이의 최소 거리(m)", example = "320")
        Integer distanceFromUserM,

        @Schema(description = "코스 난이도", nullable = true)
        Difficulty difficulty,

        @ArraySchema(schema = @Schema(implementation = CourseType.class), arraySchema = @Schema(description = "코스 유형 목록"))
        List<CourseType> courseTypes,

        @Schema(description = "코스 대표 이미지 URL", example = "https://cdn.withrun.kr/courses/12.png", nullable = true)
        String snapshotImageUrl
) {
}
