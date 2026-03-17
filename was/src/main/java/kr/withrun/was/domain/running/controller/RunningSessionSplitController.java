package kr.withrun.was.domain.running.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import kr.withrun.was.domain.running.dto.CreateRunningSessionSplitRequest;
import kr.withrun.was.domain.running.dto.CreateRunningSessionSplitResponse;
import kr.withrun.was.domain.running.service.RunningSessionSplitService;
import kr.withrun.was.global.response.ApiResponse;
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
public class RunningSessionSplitController {

    private final RunningSessionSplitService runningSessionSplitService;

    @PostMapping("/{runningSessionId}/splits")
    public ResponseEntity<ApiResponse<CreateRunningSessionSplitResponse>> createRunningSessionSplit(
            @PathVariable @Positive Long runningSessionId,
            @Valid @RequestBody CreateRunningSessionSplitRequest request
    ) {
        return ApiResponse.successEntity(
                kr.withrun.was.global.response.ResponseCode.CREATED,
                runningSessionSplitService.createRunningSessionSplit(runningSessionId, request)
        );
    }
}
