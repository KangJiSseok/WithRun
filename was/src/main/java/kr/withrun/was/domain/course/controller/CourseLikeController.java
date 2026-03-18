package kr.withrun.was.domain.course.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.withrun.was.domain.course.dto.CourseLikeStatusResponse;
import kr.withrun.was.domain.course.service.CourseLikeService;
import kr.withrun.was.global.response.ApiResponse;
import kr.withrun.was.global.response.swagger.ErrorApiResponseDoc;
import kr.withrun.was.global.response.swagger.SuccessApiResponseDocs;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Course Like", description = "코스 좋아요 추가 및 취소 API")
@Validated
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseLikeController {

    private final CourseLikeService courseLikeService;

    @Operation(
            summary = "코스에 좋아요를 누른다",
            description = "특정 사용자가 코스에 좋아요를 추가한다. 이미 좋아요가 있어도 최종 상태를 true 로 반환한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "좋아요 추가 성공",
                    content = @Content(schema = @Schema(implementation = SuccessApiResponseDocs.CourseLikeStatusApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "코스 또는 사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            )
    })
    @PostMapping("/{courseId}/like/{userId}")
    public ResponseEntity<ApiResponse<CourseLikeStatusResponse>> likeCourse(
            @Parameter(description = "좋아요를 누를 코스 ID", example = "12")
            @PathVariable Long courseId,
            @Parameter(description = "좋아요를 누를 사용자 ID", example = "7")
            @PathVariable Long userId
    ) {
        return ApiResponse.successEntity(courseLikeService.likeCourse(courseId, userId));
    }

    @Operation(
            summary = "코스 좋아요를 취소한다",
            description = "특정 사용자의 코스 좋아요를 취소한다. 좋아요가 없어도 최종 상태를 false 로 반환한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "좋아요 취소 성공",
                    content = @Content(schema = @Schema(implementation = SuccessApiResponseDocs.CourseLikeStatusApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "코스 또는 사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            )
    })
    @DeleteMapping("/{courseId}/like/{userId}")
    public ResponseEntity<ApiResponse<CourseLikeStatusResponse>> unlikeCourse(
            @Parameter(description = "좋아요를 취소할 코스 ID", example = "12")
            @PathVariable Long courseId,
            @Parameter(description = "좋아요를 취소할 사용자 ID", example = "7")
            @PathVariable Long userId
    ) {
        return ApiResponse.successEntity(courseLikeService.unlikeCourse(courseId, userId));
    }
}
