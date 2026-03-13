package kr.withrun.was.domain.running.service;

import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.repository.CourseRepository;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.running.dto.CreateRunningSessionRequest;
import kr.withrun.was.domain.running.dto.CreateRunningSessionResponse;
import kr.withrun.was.domain.running.dto.UpdateRunningSessionRequest;
import kr.withrun.was.domain.running.dto.UpdateRunningSessionResponse;
import kr.withrun.was.domain.running.entity.RunningSession;
import kr.withrun.was.domain.running.repository.RunningSessionRepository;
import kr.withrun.was.domain.running.type.RunningMode;
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
public class RunningSessionService {

    private final RunningSessionRepository runningSessionRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;

    @Transactional
    public CreateRunningSessionResponse createRunningSession(CreateRunningSessionRequest request) {
        RunningMode mode = request.mode();
        User user = getUser(request.userId());
        Course course = getCourse(mode, request.courseId());
        RunningSession ghostTargetRunningSession = getGhostTargetRunningSession(mode, request.ghostTargetRunningSessionId());

        RunningSession savedRunningSession = runningSessionRepository.save(RunningSession.start(
                user,
                mode,
                course,
                ghostTargetRunningSession,
                request.startedAt(),
                request.startLatitude(),
                request.startLongitude()
        ));

        return CreateRunningSessionResponse.from(savedRunningSession);
    }

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


    private User getUser(Long userId) {
        return userRepository.findNotDeletedUser(userId)
                .orElseThrow(() -> new CustomException(ResponseCode.USER_NOT_FOUND));
    }

    private Course getCourse(RunningMode mode, Long courseId) {
        if (mode == RunningMode.FREE) {
            return null;
        }

        if (courseId == null) {
            throw new CustomException(ResponseCode.INVALID_INPUT_VALUE);
        }

        return courseRepository.findNotDeletedCourse(courseId)
                .filter(course -> course.getStatus() == CourseStatus.OFFICIAL)
                .orElseThrow(() -> new CustomException(ResponseCode.COURSE_NOT_FOUND));
    }

    private RunningSession getGhostTargetRunningSession(RunningMode mode, Long ghostTargetRunningSessionId) {
        if (mode != RunningMode.GHOST) {
            return null;
        }

        if (ghostTargetRunningSessionId == null) {
            throw new CustomException(ResponseCode.INVALID_INPUT_VALUE);
        }

        RunningSession ghostTargetRunningSession = runningSessionRepository.findByIdAndDeletedAtIsNull(ghostTargetRunningSessionId)
                .orElseThrow(() -> new CustomException(ResponseCode.GHOST_TARGET_NOT_FOUND));

        if (!Boolean.TRUE.equals(ghostTargetRunningSession.getIsCompleted())) {
            throw new CustomException(ResponseCode.INVALID_GHOST_TARGET);
        }

        return ghostTargetRunningSession;
    }

    private RunningSession getRunningSession(Long runningSessionId) {
        RunningSession runningSession = runningSessionRepository.findByIdAndDeletedAtIsNull(runningSessionId)
                .orElseThrow(() -> new CustomException(ResponseCode.RUNNING_SESSION_NOT_FOUND));
        return runningSession;
    }
}
