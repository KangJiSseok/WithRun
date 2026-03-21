package kr.withrun.was.domain.course.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kr.withrun.was.domain.course.dto.CourseDetailResponse;
import kr.withrun.was.domain.course.dto.CourseGhostDetailResponse;
import kr.withrun.was.domain.course.dto.CourseGhostLeaderboardRequest;
import kr.withrun.was.domain.course.dto.CourseGhostLeaderboardResponse;
import kr.withrun.was.domain.course.dto.CourseLeaderboardRequest;
import kr.withrun.was.domain.course.dto.CourseLeaderboardResponse;
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
            summary = "코스 고스트 상세를 조회한다",
            description = "특정 코스의 고스트 러닝 화면에서 사용하는 코스 상세 정보와 지정한 사용자의 최고 기록 슬롯을 조회한다. 아직 인증 연동 전이라 userId path variable 을 임시로 받는다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "코스 고스트 상세 조회 성공",
                    content = @Content(schema = @Schema(implementation = SuccessApiResponseDocs.CourseGhostDetailApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "코스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            )
    })
    @GetMapping("/{courseId}/ghost-detail/{userId}")
    public ResponseEntity<ApiResponse<CourseGhostDetailResponse>> findCourseGhostDetail(
            @Parameter(description = "고스트 상세를 조회할 코스 ID", example = "12")
            @PathVariable Long courseId,
            @Parameter(description = "고스트 개인 기록을 조회할 사용자 ID", example = "7")
            @PathVariable Long userId
    ) {
        return ApiResponse.successEntity(courseService.findCourseGhostDetail(courseId, userId));
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

    @Operation(
            summary = "코스 리더보드를 조회한다",
            description = "courseId 쿼리 파라미터 기준으로 리더보드 항목을 조회하고 point 내림차순으로 rank()를 계산해 페이지 단위로 반환한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "코스 리더보드 조회 성공", useReturnTypeSchema = true),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "courseId, page, size 값이 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = ValidationErrorApiResponseDoc.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "코스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            )
    })
    @GetMapping("/leaderboards")
    public ResponseEntity<ApiResponse<CourseLeaderboardResponse>> findCourseLeaderboard(
            @Valid @ParameterObject @ModelAttribute CourseLeaderboardRequest request
    ) {
        return ApiResponse.successEntity(courseService.findCourseLeaderboard(request));
    }

    @Operation(
            summary = "코스 고스트 리더보드를 조회한다",
            description = "특정 코스의 고스트 리더보드를 조회한다. cursor 는 이전 응답의 nextCursor 값을 그대로 전달하는 불투명 토큰이며 point 내림차순, leaderboardId 오름차순 기준으로 이어진다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "코스 고스트 리더보드 조회 성공",
                    content = @Content(schema = @Schema(implementation = SuccessApiResponseDocs.CourseGhostLeaderboardApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "size 또는 cursor 값이 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = ValidationErrorApiResponseDoc.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "코스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            )
    })
    @GetMapping("/{courseId}/ghost-leaderboard")
    public ResponseEntity<ApiResponse<CourseGhostLeaderboardResponse>> findCourseGhostLeaderboard(
            @Parameter(description = "고스트 리더보드를 조회할 코스 ID", example = "12")
            @PathVariable Long courseId,
            @Valid @ParameterObject @ModelAttribute CourseGhostLeaderboardRequest request
    ) {
        return ApiResponse.successEntity(courseService.findCourseGhostLeaderboard(courseId, request));
    }
}
