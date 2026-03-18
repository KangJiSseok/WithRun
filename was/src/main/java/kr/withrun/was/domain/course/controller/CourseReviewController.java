package kr.withrun.was.domain.course.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.withrun.was.domain.course.dto.CreateCourseReviewRequest;
import kr.withrun.was.domain.course.dto.CreateCourseReviewResponse;
import kr.withrun.was.domain.course.service.CourseReviewService;
import kr.withrun.was.global.response.ApiResponse;
import kr.withrun.was.global.response.swagger.ErrorApiResponseDoc;
import kr.withrun.was.global.response.swagger.ValidationErrorApiResponseDoc;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static kr.withrun.was.global.response.ResponseCode.CREATED;

@Tag(name = "Course Review", description = "코스 리뷰 작성 API")
@Validated
@RestController
@RequestMapping("/api/courses")
@RequiredArgsConstructor
public class CourseReviewController {

    private final CourseReviewService courseReviewService;

    @Operation(
            summary = "코스 리뷰를 작성한다",
            description = "특정 코스에 대한 평점, 유형 태그, 체감 난이도를 저장한다. 같은 사용자는 같은 코스에 한 번만 리뷰를 작성할 수 있다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "코스 리뷰 작성 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 본문 형식, 평점, 유형 태그 또는 난이도 값이 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = ValidationErrorApiResponseDoc.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "코스 또는 사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 리뷰를 작성한 코스임",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            )
    })
    @PostMapping("/{courseId}/reviews")
    public ResponseEntity<ApiResponse<CreateCourseReviewResponse>> createCourseReview(
            @Parameter(description = "리뷰를 작성할 코스 ID", example = "12")
            @PathVariable Long courseId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "코스 리뷰 작성 정보"
            )
            @Valid @RequestBody CreateCourseReviewRequest request
    ) {
        return ApiResponse.successEntity(CREATED, courseReviewService.createCourseReview(courseId, request));
    }
}
