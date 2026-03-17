package kr.withrun.was.domain.running.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import kr.withrun.was.domain.running.dto.CreateRunningHealthSampleBatchRequest;
import kr.withrun.was.domain.running.dto.CreateRunningHealthSampleBatchResponse;
import kr.withrun.was.domain.running.service.RunningHealthSampleService;
import kr.withrun.was.global.response.ApiResponse;
import kr.withrun.was.global.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/running-sessions")
@RequiredArgsConstructor
public class RunningHealthSampleController {

    private final RunningHealthSampleService runningHealthSampleService;

    @PostMapping("/{runningSessionId}/health-samples/batch")
    public ResponseEntity<ApiResponse<CreateRunningHealthSampleBatchResponse>> batch(
            @PathVariable @Positive Long runningSessionId,
            @Valid @RequestBody CreateRunningHealthSampleBatchRequest request
    ) {
        CreateRunningHealthSampleBatchResponse response =
                runningHealthSampleService.createRunningHealthSampleBatch(runningSessionId, request);

        return ApiResponse.successEntity(ResponseCode.CREATED, response);
    }
}
