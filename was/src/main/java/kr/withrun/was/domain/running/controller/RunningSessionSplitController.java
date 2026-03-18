package kr.withrun.was.domain.running.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import kr.withrun.was.domain.running.dto.CreateRunningSessionSplitRequest;
import kr.withrun.was.domain.running.dto.CreateRunningSessionSplitResponse;
import kr.withrun.was.domain.running.service.RunningSessionSplitService;
import kr.withrun.was.global.response.ApiResponse;
import kr.withrun.was.global.response.swagger.ErrorApiResponseDoc;
import kr.withrun.was.global.response.swagger.SuccessApiResponseDocs;
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

@Tag(name = "Running Session Split", description = "러닝 세션 구간 기록 API")
@Validated
@RestController
@RequestMapping("/api/running-sessions")
@RequiredArgsConstructor
public class RunningSessionSplitController {

    private final RunningSessionSplitService runningSessionSplitService;

    @Operation(
            summary = "러닝 세션 구간 기록을 저장한다",
            description = "특정 러닝 세션의 구간별 거리, 시간, 페이스, 평균 심박수 같은 split 기록을 저장한다. splitIndex 는 1부터 시작하는 구간 순번이다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "구간 기록 저장 성공",
                    content = @Content(schema = @Schema(implementation = SuccessApiResponseDocs.CreateRunningSessionSplitApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "runningSessionId 또는 구간 기록 값이 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = ValidationErrorApiResponseDoc.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자 또는 러닝 세션을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            )
    })
    @PostMapping("/{runningSessionId}/splits")
    public ResponseEntity<ApiResponse<CreateRunningSessionSplitResponse>> createRunningSessionSplit(
            @Parameter(description = "구간 기록을 저장할 러닝 세션 ID", example = "91")
            @PathVariable @Positive Long runningSessionId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "사용자 ID와 구간별 러닝 기록을 담은 요청 본문"
            )
            @Valid @RequestBody CreateRunningSessionSplitRequest request
    ) {
        return ApiResponse.successEntity(
                CREATED,
                runningSessionSplitService.createRunningSessionSplit(runningSessionId, request)
        );
    }
}
