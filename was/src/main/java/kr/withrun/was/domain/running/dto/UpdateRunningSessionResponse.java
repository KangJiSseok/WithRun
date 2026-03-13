package kr.withrun.was.domain.running.dto;

import kr.withrun.was.domain.running.entity.RunningSession;

import java.time.LocalDateTime;

public record UpdateRunningSessionResponse(
        Long runningSessionId,
        boolean isCompleted,
        boolean isPublic,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        int distanceM,
        int caloriesKcal,
        Double endLatitude,
        Double endLongitude,
        Double avgSpeedMps,
        Integer durationSec,
        Integer avgPaceSecPerKm,
        double startLatitude,
        double startLongitude,
        int elevationGainM
) {

    public static UpdateRunningSessionResponse from(RunningSession runningSession) {
        return new UpdateRunningSessionResponse(
                runningSession.getId(),
                runningSession.getIsCompleted(),
                runningSession.getIsPublic(),
                runningSession.getStartedAt(),
                runningSession.getEndedAt(),
                runningSession.getDistanceM(),
                runningSession.getCaloriesKcal(),
                runningSession.getEndLatitude(),
                runningSession.getEndLongitude(),
                runningSession.getAvgSpeedMps(),
                runningSession.getDurationSec(),
                runningSession.getAvgPaceSecPerKm(),
                runningSession.getStartLatitude(),
                runningSession.getStartLongitude(),
                runningSession.getElevationGainM()
        );
    }
}
