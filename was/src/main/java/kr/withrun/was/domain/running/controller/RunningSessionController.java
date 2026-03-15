package kr.withrun.was.domain.running.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;
import kr.withrun.was.domain.running.dto.CreateRunningSessionRequest;
import kr.withrun.was.domain.running.dto.CreateRunningSessionResponse;
import kr.withrun.was.domain.running.dto.AllRunningSessionsRequest;
import kr.withrun.was.domain.running.dto.AllRunningSessionsResponse;
import kr.withrun.was.domain.running.dto.UpdateRunningSessionRequest;
import kr.withrun.was.domain.running.dto.UpdateRunningSessionResponse;
import kr.withrun.was.domain.running.service.RunningSessionService;
import kr.withrun.was.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static kr.withrun.was.global.response.ResponseCode.CREATED;

@Validated
@RestController
@RequestMapping("/api/running-sessions")
@RequiredArgsConstructor
public class RunningSessionController {

    private final RunningSessionService runningSessionService;

    @PostMapping
    public ResponseEntity<ApiResponse<CreateRunningSessionResponse>> createRunningSession(
            @Valid @RequestBody CreateRunningSessionRequest request
    ) {
        return ApiResponse.successEntity(CREATED, runningSessionService.createRunningSession(request));
    }

    @PatchMapping("/{runningSessionId}")
    public ResponseEntity<ApiResponse<UpdateRunningSessionResponse>> completeRunningSession(
            @PathVariable Long runningSessionId,
            @Valid @RequestBody UpdateRunningSessionRequest request
    ) {
        return ApiResponse.successEntity(runningSessionService.completeRunningSession(runningSessionId, request));
    }

    @GetMapping("/history/{userId}/all")
    public ResponseEntity<ApiResponse<AllRunningSessionsResponse>> findAllRunningSessions(
            @PathVariable @Positive Long userId,
            @RequestParam(defaultValue = "10") @Positive @Max(50) Integer pageSize,
            @RequestParam(required = false) String cursor
    ) {
        return ApiResponse.successEntity(
                runningSessionService.findAllRunningSessions(
                        new AllRunningSessionsRequest(userId, pageSize, cursor)
                )
        );
    }
}
