package kr.withrun.was.domain.running.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import kr.withrun.was.domain.running.dto.CreateRunningGpsSampleBatchRequest;
import kr.withrun.was.domain.running.dto.CreateRunningGpsSampleBatchResponse;
import kr.withrun.was.domain.running.service.RunningGpsSampleService;
import kr.withrun.was.global.response.ApiResponse;
import kr.withrun.was.global.response.ResponseCode;
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

@Tag(name = "Running GPS Sample", description = "러닝 세션 GPS 샘플 적재 API")
@Validated
@RestController
@RequestMapping("/api/running-sessions")
@RequiredArgsConstructor
public class RunningGpsSampleController {

    private final RunningGpsSampleService runningGpsSampleService;

    @Operation(
            summary = "GPS 샘플 배치를 저장한다",
            description = "러닝 세션 중 수집한 GPS 샘플을 한 번에 저장한다. 각 샘플에는 좌표, 고도, 정확도, 방위각, 수집 시각이 포함된다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "GPS 샘플 배치 저장 성공",
                    useReturnTypeSchema = true
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "runningSessionId, userId 또는 GPS 샘플 값이 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = ValidationErrorApiResponseDoc.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자 또는 러닝 세션을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            )
    })
    @PostMapping("/{runningSessionId}/gps-samples/batch")
    public ResponseEntity<ApiResponse<CreateRunningGpsSampleBatchResponse>> batch(
            @Parameter(description = "GPS 샘플을 적재할 러닝 세션 ID", example = "91")
            @PathVariable @Positive Long runningSessionId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "사용자 ID와 GPS 샘플 배열을 담은 요청 본문"
            )
            @Valid @RequestBody CreateRunningGpsSampleBatchRequest request
    ) {
        CreateRunningGpsSampleBatchResponse response =
                runningGpsSampleService.createRunningGpsSampleBatch(runningSessionId, request);

        return ApiResponse.successEntity(ResponseCode.CREATED, response);
    }
}
