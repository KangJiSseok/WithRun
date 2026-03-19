package kr.withrun.was.domain.running.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import kr.withrun.was.domain.running.dto.CreateRunningSessionDetailsRequest;
import kr.withrun.was.domain.running.dto.CreateRunningSessionDetailsResponse;
import kr.withrun.was.domain.running.dto.CreateRunningSessionRequest;
import kr.withrun.was.domain.running.dto.CreateRunningSessionResponse;
import kr.withrun.was.domain.running.dto.PastRunningSessionsRequest;
import kr.withrun.was.domain.running.dto.PastRunningSessionsResponse;
import kr.withrun.was.domain.running.dto.RegisterRunningSessionCourseRequest;
import kr.withrun.was.domain.running.dto.RegisterRunningSessionCourseResponse;
import kr.withrun.was.domain.running.dto.RunningSessionDetailRequest;
import kr.withrun.was.domain.running.dto.RunningSessionDetailResponse;
import kr.withrun.was.domain.running.dto.UpdateRunningSessionRequest;
import kr.withrun.was.domain.running.dto.UpdateRunningSessionResponse;
import kr.withrun.was.domain.running.service.RunningSessionService;
import kr.withrun.was.global.response.ApiResponse;
import kr.withrun.was.global.response.swagger.ErrorApiResponseDoc;
import kr.withrun.was.global.response.swagger.SuccessApiResponseDocs;
import kr.withrun.was.global.response.swagger.ValidationErrorApiResponseDoc;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static kr.withrun.was.global.response.ResponseCode.CREATED;

@Validated
@Tag(name = "Running Session", description = "러닝 세션 생성, 종료, 조회 API")
@RestController
@RequestMapping("/api/running-sessions")
@RequiredArgsConstructor
public class RunningSessionController {

    private final RunningSessionService runningSessionService;

    @Operation(
            summary = "러닝 세션을 시작한다",
            description = "사용자와 러닝 모드 기준으로 새로운 러닝 세션을 생성한다. COURSE 모드에서는 courseId 가 필요하고, GHOST 모드에서는 ghostTargetRunningSessionId 가 필요하다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "러닝 세션 생성 성공",
                    content = @Content(schema = @Schema(implementation = SuccessApiResponseDocs.CreateRunningSessionApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 본문 형식, 좌표 범위, 모드별 필수 필드 또는 고스트 대상 조건이 올바르지 않음",
                    content = @Content(schema = @Schema(implementation = ValidationErrorApiResponseDoc.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자, 코스 또는 고스트 대상 러닝 세션을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            )
    })
    @PostMapping
    public ResponseEntity<ApiResponse<CreateRunningSessionResponse>> createRunningSession(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "러닝 시작 시점의 사용자, 모드, 시작 좌표를 담은 요청 본문"
            )
            @Valid @RequestBody CreateRunningSessionRequest request
    ) {
        return ApiResponse.successEntity(CREATED, runningSessionService.createRunningSession(request));
    }

    @Operation(
            summary = "러닝 세션을 종료한다",
            description = "진행 중인 러닝 세션을 종료하고 최종 거리, 칼로리, 페이스 등 요약 기록을 확정한다. 이미 종료된 세션은 다시 종료할 수 없다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "러닝 세션 종료 성공",
                    content = @Content(schema = @Schema(implementation = SuccessApiResponseDocs.UpdateRunningSessionApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 본문 형식, 좌표 범위 또는 종료 기록 값이 올바르지 않음",
                    content = @Content(schema = @Schema(implementation = ValidationErrorApiResponseDoc.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "러닝 세션을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "이미 종료된 러닝 세션임",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            )
    })
    @PatchMapping("/{runningSessionId}")
    public ResponseEntity<ApiResponse<UpdateRunningSessionResponse>> completeRunningSession(
            @Parameter(description = "종료 처리할 러닝 세션 ID", example = "91")
            @PathVariable Long runningSessionId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "러닝 종료 시점의 최종 기록을 담은 요청 본문"
            )
            @Valid @RequestBody UpdateRunningSessionRequest request
    ) {
        return ApiResponse.successEntity(runningSessionService.completeRunningSession(runningSessionId, request));
    }

    @Operation(
            summary = "과거 러닝 세션을 조회한다",
            description = "사용자 기준으로 과거 러닝 세션 목록을 조회한다. year, month, day 는 날짜 필터이고 cursor 는 이전 응답의 nextCursor 값을 그대로 전달하는 불투명 토큰이다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "과거 러닝 세션 조회 성공",
                    content = @Content(schema = @Schema(implementation = SuccessApiResponseDocs.PastRunningSessionsApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "userId, 날짜 필터 조합, pageSize 또는 cursor 값이 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = ValidationErrorApiResponseDoc.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            )
    })
    @GetMapping("/history/past")
    public ResponseEntity<ApiResponse<PastRunningSessionsResponse>> findPastRunningSessions(
            @Valid @ParameterObject @ModelAttribute PastRunningSessionsRequest request
    ) {
        return ApiResponse.successEntity(runningSessionService.findPastRunningSessions(request));
    }

    @Operation(
            summary = "완료한 러닝 기록을 코스로 등록한다",
            description = "완료된 러닝 세션 기록을 바탕으로 커뮤니티 코스를 생성한다. 난이도와 코스 유형은 문자열 enum 이름으로 전달하며 snapshotImageUrl 을 비우면 세션 스냅샷을 재사용한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "러닝 기록 코스 등록 성공",
                    content = @Content(schema = @Schema(implementation = SuccessApiResponseDocs.RegisterRunningSessionCourseApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "요청 본문 형식, 난이도 또는 코스 유형 값이 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = ValidationErrorApiResponseDoc.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "러닝 세션을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "아직 완료되지 않은 러닝 세션이라 코스로 등록할 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            )
    })
    @PostMapping("/{runningSessionId}/register-course")
    public ResponseEntity<ApiResponse<RegisterRunningSessionCourseResponse>> registerRunningSessionCourse(
            @Parameter(description = "코스로 등록할 완료된 러닝 세션 ID", example = "91")
            @PathVariable Long runningSessionId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "코스 제목, 난이도, 코스 유형, 스냅샷 이미지 URL을 담은 요청 본문"
            )
            @Valid @RequestBody RegisterRunningSessionCourseRequest request
    ) {
        return ApiResponse.successEntity(CREATED, runningSessionService.registerRunningSessionCourse(runningSessionId, request));
    }

    @PostMapping("/{runningSessionId}/detail")
    public ResponseEntity<ApiResponse<CreateRunningSessionDetailsResponse>> createRunningSessionDetails(
            @PathVariable @Positive Long runningSessionId,
            @Valid @RequestBody CreateRunningSessionDetailsRequest request
    ) {
        return ApiResponse.successEntity(CREATED, runningSessionService.createRunningSessionDetail(runningSessionId, request));
    }

    @GetMapping("/{runningSessionId}/detail")
    public ResponseEntity<ApiResponse<RunningSessionDetailResponse>> findRunningSessionDetail(
            @PathVariable @Positive Long runningSessionId,
            @Valid @RequestBody RunningSessionDetailRequest request
    ) {
        return ApiResponse.successEntity(runningSessionService.findRunningSessionDetail(runningSessionId, request.userId()));
    }
}
