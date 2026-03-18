package kr.withrun.was.domain.running.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import kr.withrun.was.domain.running.dto.CreateRunningHealthSampleBatchRequest;
import kr.withrun.was.domain.running.dto.CreateRunningHealthSampleBatchResponse;
import kr.withrun.was.domain.running.service.RunningHealthSampleService;
import kr.withrun.was.global.response.ApiResponse;
import kr.withrun.was.global.response.ResponseCode;
import kr.withrun.was.global.response.swagger.ErrorApiResponseDoc;
import kr.withrun.was.global.response.swagger.SuccessApiResponseDocs;
import kr.withrun.was.global.response.swagger.ValidationErrorApiResponseDoc;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Running Health Sample", description = "러닝 세션 건강 샘플 적재 API")
@Validated
@RestController
@RequestMapping("/api/running-sessions")
public class RunningHealthSampleController {

    private final RunningHealthSampleService runningHealthSampleService;

    public RunningHealthSampleController(RunningHealthSampleService runningHealthSampleService) {
        this.runningHealthSampleService = runningHealthSampleService;
    }

    @Operation(
            summary = "건강 샘플 배치를 저장한다",
            description = "러닝 세션 중 수집한 심박, 속도, 페이스, 칼로리, 케이던스 샘플을 한 번에 저장한다. samples 는 시간순으로 전달하는 것을 권장한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "건강 샘플 배치 저장 성공",
                    content = @Content(schema = @Schema(implementation = SuccessApiResponseDocs.CreateRunningHealthSampleBatchApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "runningSessionId, userId 또는 샘플 목록 값이 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = ValidationErrorApiResponseDoc.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자 또는 러닝 세션을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            )
    })
    @PostMapping("/{runningSessionId}/health-samples/batch")
    public ResponseEntity<ApiResponse<CreateRunningHealthSampleBatchResponse>> batch(
            @Parameter(description = "건강 샘플을 적재할 러닝 세션 ID", example = "91")
            @PathVariable @Positive Long runningSessionId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "사용자 ID와 건강 샘플 배열을 담은 요청 본문"
            )
            @Valid @RequestBody CreateRunningHealthSampleBatchRequest request
    ) {
        CreateRunningHealthSampleBatchResponse response =
                runningHealthSampleService.createRunningHealthSampleBatch(runningSessionId, request);

        return ApiResponse.successEntity(ResponseCode.CREATED, response);
    }
}
