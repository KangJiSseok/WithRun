package kr.withrun.was.domain.course.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(description = "코스 리더보드 조회 조건")
public record CourseLeaderboardRequest(

        @Schema(description = "리더보드를 조회할 코스 ID", example = "12", minimum = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Positive
        Long courseId,

        @Schema(description = "0부터 시작하는 페이지 번호입니다. 생략하면 0을 사용합니다.", example = "0", defaultValue = "0", minimum = "0")
        @PositiveOrZero
        Integer page,

        @Schema(description = "한 번에 조회할 최대 항목 수입니다. 생략하면 10을 사용합니다.", example = "10", defaultValue = "10", minimum = "1", maximum = "100")
        @Positive
        @Max(100)
        Integer size
) {

    public CourseLeaderboardRequest {
        page = page == null ? 0 : page;
        size = size == null ? 10 : size;
    }
}
