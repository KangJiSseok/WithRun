package kr.withrun.was.domain.running.service;

import kr.withrun.was.domain.running.dto.CreateRunningSessionSplitRequest;
import kr.withrun.was.domain.running.dto.CreateRunningSessionSplitResponse;
import kr.withrun.was.domain.running.entity.RunningSession;
import kr.withrun.was.domain.running.entity.RunningSessionSplit;
import kr.withrun.was.domain.running.repository.RunningSessionRepository;
import kr.withrun.was.domain.running.repository.RunningSessionSplitRepository;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.repository.UserRepository;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RunningSessionSplitService {

    private final RunningSessionSplitRepository runningSessionSplitRepository;
    private final RunningSessionRepository runningSessionRepository;
    private final UserRepository userRepository;

    @Transactional
    public CreateRunningSessionSplitResponse createRunningSessionSplit(
            Long runningSessionId,
            CreateRunningSessionSplitRequest request
    ) {
        User user = getUser(request.userId());
        RunningSession runningSession = getRunningSession(runningSessionId);
        runningSession.validateOwner(user.getId());

        RunningSessionSplit runningSessionSplit = runningSessionSplitRepository.save(
                RunningSessionSplit.create(
                        runningSession,
                        request.splitIndex(),
                        request.splitDistanceM(),
                        request.splitDurationSec(),
                        request.splitPaceSecPerKm(),
                        request.avgHeartRate(),
                        request.elevationGainM()
                )
        );

        return CreateRunningSessionSplitResponse.from(runningSessionSplit);
    }

    private User getUser(Long userId) {
        return userRepository.findNotDeletedUser(userId)
                .orElseThrow(() -> new CustomException(ResponseCode.USER_NOT_FOUND));
    }

    private RunningSession getRunningSession(Long runningSessionId) {
        return runningSessionRepository.findByIdAndDeletedAtIsNull(runningSessionId)
                .orElseThrow(() -> new CustomException(ResponseCode.RUNNING_SESSION_NOT_FOUND));
    }
}
