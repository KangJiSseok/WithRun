package kr.withrun.was.domain.running.controller;

import jakarta.validation.Valid;
import kr.withrun.was.domain.running.dto.UpdateRunningSessionRequest;
import kr.withrun.was.domain.running.dto.UpdateRunningSessionResponse;
import kr.withrun.was.domain.running.service.RunningSessionService;
import kr.withrun.was.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/running-sessions")
@RequiredArgsConstructor
public class RunningSessionController {

    private final RunningSessionService runningSessionService;

    @PatchMapping("/{runningSessionId}")
    public ResponseEntity<ApiResponse<UpdateRunningSessionResponse>> completeRunningSession(
            @PathVariable Long runningSessionId,
            @Valid @RequestBody UpdateRunningSessionRequest request
    ) {
        return ApiResponse.successEntity(runningSessionService.completeRunningSession(runningSessionId, request));
    }
}
