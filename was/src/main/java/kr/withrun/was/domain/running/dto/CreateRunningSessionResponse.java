package kr.withrun.was.domain.running.dto;

import kr.withrun.was.domain.running.entity.RunningSession;
import kr.withrun.was.domain.running.type.RunningMode;

public record CreateRunningSessionResponse(
        Long runningSessionId,
        RunningMode mode,
        Long courseId,
        Long ghostTargetRunningSessionId,
        int distanceM,
        int caloriesKcal,
        boolean isCompleted
) {

    public static CreateRunningSessionResponse from(RunningSession runningSession) {
        return new CreateRunningSessionResponse(
                runningSession.getId(),
                runningSession.getMode(),
                runningSession.getCourse() != null
                        ? runningSession.getCourse().getId()
                        : null,
                runningSession.getGhostTargetRunningSession() != null
                        ? runningSession.getGhostTargetRunningSession().getId()
                        : null,
                runningSession.getDistanceM(),
                runningSession.getCaloriesKcal(),
                Boolean.TRUE.equals(runningSession.getIsCompleted())
        );
    }
}
