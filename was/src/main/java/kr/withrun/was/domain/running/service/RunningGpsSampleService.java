package kr.withrun.was.domain.running.service;

import kr.withrun.was.domain.running.dto.CreateRunningGpsSampleBatchRequest;
import kr.withrun.was.domain.running.dto.CreateRunningGpsSampleBatchResponse;
import kr.withrun.was.domain.running.entity.RunningGpsSample;
import kr.withrun.was.domain.running.entity.RunningSession;
import kr.withrun.was.domain.running.repository.RunningGpsSampleRepository;
import kr.withrun.was.domain.running.repository.RunningSessionRepository;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.repository.UserRepository;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RunningGpsSampleService {

    private final RunningGpsSampleRepository runningGpsSampleRepository;
    private final UserRepository userRepository;
    private final RunningSessionRepository runningSessionRepository;

    @Transactional
    public CreateRunningGpsSampleBatchResponse createRunningGpsSampleBatch(
            Long runningSessionId,
            CreateRunningGpsSampleBatchRequest request
    ) {
        User user = getUser(request.userId());
        RunningSession runningSession = getRunningSession(runningSessionId);
        runningSession.validateOwner(user.getId());

        List<RunningGpsSample> runningGpsSamples = request.samples().stream()
                .map(sample -> RunningGpsSample.create(
                        user,
                        runningSession,
                        sample.bearingDeg(),
                        sample.accuracyM(),
                        sample.altitudeM(),
                        sample.longitude(),
                        sample.latitude(),
                        sample.sampledAt()
                ))
                .toList();

        runningGpsSampleRepository.saveAll(runningGpsSamples);

        return CreateRunningGpsSampleBatchResponse.of(
                runningSession.getId(),
                user.getId(),
                runningGpsSamples.size()
        );
    }

    /**   각 도매인 repository에서 find..() 하는 메서드   */

    private User getUser(Long userId) {
        return userRepository.findNotDeletedUser(userId)
                .orElseThrow(() -> new CustomException(ResponseCode.USER_NOT_FOUND));
    }

    private RunningSession getRunningSession(Long runningSessionId) {
        return runningSessionRepository.findByIdAndDeletedAtIsNull(runningSessionId)
                .orElseThrow(() -> new CustomException(ResponseCode.RUNNING_SESSION_NOT_FOUND));
    }

}
