package kr.withrun.was.domain.running.service;

import kr.withrun.was.domain.running.dto.CreateRunningHealthSampleBatchRequest;
import kr.withrun.was.domain.running.dto.CreateRunningHealthSampleBatchResponse;
import kr.withrun.was.domain.running.entity.RunningHealthSample;
import kr.withrun.was.domain.running.entity.RunningSession;
import kr.withrun.was.domain.running.repository.RunningHealthSampleRepository;
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
public class RunningHealthSampleService {

    private final UserRepository userRepository;
    private final RunningSessionRepository runningSessionRepository;
    private final RunningHealthSampleRepository runningHealthSampleRepository;

    @Transactional
    public CreateRunningHealthSampleBatchResponse createRunningHealthSampleBatch(
            Long runningSessionId,
            CreateRunningHealthSampleBatchRequest request
    ) {
        User user = getUser(request.userId());
        RunningSession runningSession = getRunningSession(runningSessionId);
        runningSession.validateOwner(user.getId());

        List<RunningHealthSample> runningHealthSamples = request.samples().stream()
                .map(sample -> RunningHealthSample.create(
                        user,
                        runningSession,
                        sample.heartRate(),
                        sample.speedMps(),
                        sample.paceSecPerKm(),
                        sample.distanceM(),
                        sample.caloriesKcal(),
                        sample.cadenceSpm(),
                        sample.sampledAt()
                ))
                .toList();

        runningHealthSampleRepository.saveAll(runningHealthSamples);

        return CreateRunningHealthSampleBatchResponse.of(
                runningSession.getId(),
                user.getId(),
                runningHealthSamples.size()
        );
    }

    /**
     * 각 도매인 repository에서 find..() 하는 메서드
     */

    private User getUser(Long userId) {
        return userRepository.findNotDeletedUser(userId)
                .orElseThrow(() -> new CustomException(ResponseCode.USER_NOT_FOUND));
    }

    private RunningSession getRunningSession(Long runningSessionId) {
        return runningSessionRepository.findByIdAndDeletedAtIsNull(runningSessionId)
                .orElseThrow(() -> new CustomException(ResponseCode.RUNNING_SESSION_NOT_FOUND));
    }
}
