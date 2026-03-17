package kr.withrun.was.domain.course.type;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "코스 유형 분류")
public enum CourseType {
    @Schema(description = "강변 중심 코스")
    RIVERSIDE(1, "RIVERSIDE"),

    @Schema(description = "공원 중심 코스")
    PARK(2, "PARK"),

    @Schema(description = "산책로 또는 트레일 중심 코스")
    MOUNTAIN_TRAIL(3, "MOUNTAIN_TRAIL"),

    @Schema(description = "트랙 중심 코스")
    TRACK(4, "TRACK"),

    @Schema(description = "도심형 코스")
    URBAN(5, "URBAN"),

    @Schema(description = "기타 유형 코스")
    OTHER(6, "OTHER");

    private final int index;
    private final String label;

}
