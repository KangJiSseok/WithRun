package kr.withrun.was.domain.running.service;

import kr.withrun.was.domain.running.dto.UpdateRunningSessionRequest;
import kr.withrun.was.domain.running.dto.UpdateRunningSessionResponse;
import kr.withrun.was.domain.running.entity.RunningSession;
import kr.withrun.was.domain.running.repository.RunningSessionRepository;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RunningSessionService {

    private final RunningSessionRepository runningSessionRepository;

    @Transactional
    public UpdateRunningSessionResponse completeRunningSession(Long runningSessionId, UpdateRunningSessionRequest request) {
        RunningSession runningSession = getRunningSession(runningSessionId);
        runningSession.validateOwner(request.userId());
        runningSession.validateCompletable();

        runningSession.complete(
                request.endedAt(),
                request.distanceM(),
                request.caloriesKcal(),
                request.endLatitude(),
                request.endLongitude(),
                request.avgSpeedMps(),
                request.durationSec(),
                request.avgPaceSecPerKm(),
                request.elevationGainM()
        );

        return UpdateRunningSessionResponse.from(runningSession);
    }

    private RunningSession getRunningSession(Long runningSessionId) {
        RunningSession runningSession = runningSessionRepository.findNotDeletedRunningSession(runningSessionId)
                .orElseThrow(() -> new CustomException(ResponseCode.RUNNING_SESSION_NOT_FOUND));
        return runningSession;
    }

}
