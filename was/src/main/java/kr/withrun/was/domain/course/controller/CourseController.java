package kr.withrun.was.domain.course.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.withrun.was.domain.course.dto.CourseDetailResponse;
import kr.withrun.was.domain.course.dto.CourseSurveyResponse;
import kr.withrun.was.domain.course.dto.NearbyCoursesRequest;
import kr.withrun.was.domain.course.dto.NearbyCoursesResponse;
import kr.withrun.was.domain.course.service.CourseService;
import kr.withrun.was.global.response.ApiResponse;
import kr.withrun.was.global.response.swagger.ErrorApiResponseDoc;
import kr.withrun.was.global.response.swagger.SuccessApiResponseDocs;
import kr.withrun.was.global.response.swagger.ValidationErrorApiResponseDoc;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Course", description = "코스 조회 및 추천 API")
@Validated
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @Operation(
            summary = "코스 설문 항목을 조회한다",
            description = "코스 유형과 난이도 설문에 사용할 선택 항목 목록을 조회한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "코스 설문 항목 조회 성공",
                    content = @Content(schema = @Schema(implementation = SuccessApiResponseDocs.CourseSurveyApiResponse.class))
            )
    })
    @GetMapping("/surveys")
    public ResponseEntity<ApiResponse<CourseSurveyResponse>> findCourseSurveys() {
        return ApiResponse.successEntity(courseService.findCourseSurveys());
    }

    @Operation(
            summary = "코스 상세를 조회한다",
            description = "특정 코스의 거리, 난이도, 경로 좌표, 좋아요 수 같은 상세 정보를 조회한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "코스 상세 조회 성공",
                    content = @Content(schema = @Schema(implementation = SuccessApiResponseDocs.CourseDetailApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "코스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            )
    })
    @GetMapping("/{courseId}")
    public ResponseEntity<ApiResponse<CourseDetailResponse>> findCourseDetail(
            @Parameter(description = "상세 조회할 코스 ID", example = "12")
            @PathVariable Long courseId
    ) {
        return ApiResponse.successEntity(courseService.findCourseDetail(courseId));
    }

    @Operation(
            summary = "주변 코스를 조회한다",
            description = "현재 좌표, 탐색 반경, 목표 거리 조건으로 주변 코스를 조회한다. cursor 는 이전 응답의 nextCursor 값을 그대로 전달하는 불투명 토큰이다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "주변 코스 조회 성공",
                    content = @Content(schema = @Schema(implementation = SuccessApiResponseDocs.NearbyCoursesApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "좌표, 반경, 목표 거리 또는 cursor 값이 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = ValidationErrorApiResponseDoc.class))
            )
    })
    @GetMapping("/nearby")
    public ResponseEntity<ApiResponse<NearbyCoursesResponse>> findNearbyCourses(
            @Valid @ParameterObject @ModelAttribute NearbyCoursesRequest request
    ) {
        return ApiResponse.successEntity(courseService.findNearbyCourses(request));
    }
}
