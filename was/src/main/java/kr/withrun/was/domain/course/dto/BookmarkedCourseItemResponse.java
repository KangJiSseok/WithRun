package kr.withrun.was.domain.course.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.global.common.type.Difficulty;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "북마크한 코스 단건 정보")
public record BookmarkedCourseItemResponse(
        @Schema(description = "북마크 ID", example = "42")
        Long bookmarkId,

        @Schema(description = "북마크 생성 시각", example = "2026-03-17T10:15:30")
        LocalDateTime bookmarkedAt,

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

        @Schema(description = "코스 난이도", nullable = true)
        Difficulty difficulty,

        @ArraySchema(schema = @Schema(implementation = CourseType.class), arraySchema = @Schema(description = "코스 유형 목록"))
        List<CourseType> courseTypes,

        @Schema(description = "코스 대표 이미지 URL", example = "https://cdn.withrun.kr/courses/12.png", nullable = true)
        String snapshotImageUrl,

        @Schema(description = "북마크 목록 응답에서는 항상 true 다.", example = "true")
        boolean isBookmarked,

        @Schema(description = "현재 상태에서 실제 주행 가능한 코스인지 여부. OFFICIAL 상태일 때 true 다.", example = "true")
        boolean isRunnable
) {
}
