package kr.withrun.was.domain.course.type;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "코스 공개 상태. OFFICIAL 은 공식 추천 코스, COMMUNITY 는 커뮤니티 코스, PRIVATE 는 비공개 코스다.")
public enum CourseStatus {
    @Schema(description = "공식 추천 코스")
    OFFICIAL,

    @Schema(description = "커뮤니티 공개 코스")
    COMMUNITY,

    @Schema(description = "비공개 코스")
    PRIVATE
}
