package kr.withrun.was.domain.course.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "코스 필터 설문 항목 응답")
public record CourseFilterResponse(

        @ArraySchema(schema = @Schema(implementation = CourseFilterListResponse.class), arraySchema = @Schema(description = "코스 거리 항목"))
        List<CourseFilterListResponse> courseDistanceTypes,

        @ArraySchema(schema = @Schema(implementation = CourseFilterListResponse.class), arraySchema = @Schema(description = "코스 유형 설문 항목"))
        List<CourseFilterListResponse> courseTypes,

        @ArraySchema(schema = @Schema(implementation = CourseFilterListResponse.class), arraySchema = @Schema(description = "난이도 설문 항목"))
        List<CourseFilterListResponse> difficulties
) {
    @Schema(name = "CourseFilterListItem", description = "코드 필터 선택 항목")
    public record CourseFilterListResponse(
            @Schema(description = "클라이언트가 사용하는 선택 항목 인덱스", example = "1")
            Integer index,

            @Schema(description = "선택 항목 라벨", example = "RIVERSIDE")
            String label
    ) {
    }
}


