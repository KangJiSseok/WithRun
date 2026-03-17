package kr.withrun.was.domain.running.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import kr.withrun.was.domain.running.dto.CreateRunningGpsSampleBatchRequest;
import kr.withrun.was.domain.running.dto.CreateRunningGpsSampleBatchResponse;
import kr.withrun.was.domain.running.service.RunningGpsSampleService;
import kr.withrun.was.global.response.ApiResponse;
import kr.withrun.was.global.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/running-sessions")
@RequiredArgsConstructor
public class RunningGpsSampleController {

    private final RunningGpsSampleService runningGpsSampleService;

    @PostMapping("/{runningSessionId}/gps-samples/batch")
    public ResponseEntity<ApiResponse<CreateRunningGpsSampleBatchResponse>> batch(
            @PathVariable @Positive Long runningSessionId,
            @Valid @RequestBody CreateRunningGpsSampleBatchRequest request
    ) {
        CreateRunningGpsSampleBatchResponse response =
                runningGpsSampleService.createRunningGpsSampleBatch(runningSessionId, request);

        return ApiResponse.successEntity(ResponseCode.CREATED, response);
    }
}
