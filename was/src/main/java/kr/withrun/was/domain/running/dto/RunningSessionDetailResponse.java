package kr.withrun.was.domain.running.dto;

import kr.withrun.was.domain.running.entity.RunningSession;

import java.time.LocalDateTime;
import java.util.List;

public record RunningSessionDetailResponse(
        Long runningSessionId,
        Long userId,
        String snapshotImageUrl,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Integer distanceM,
        Integer durationSec,
        Double avgSpeedMps,
        Integer avgPaceSecPerKm,
        Integer caloriesKcal,
        Integer elevationGainM,
        List<RunningGpsSampleResponse> gpsSamples,
        List<RunningHealthSampleResponse> healthSamples,
        List<RunningSessionSplitResponse> splits
) {
    public static RunningSessionDetailResponse of(
            RunningSession runningSession,
            List<RunningGpsSampleResponse> gpsSamples,
            List<RunningHealthSampleResponse> healthSamples,
            List<RunningSessionSplitResponse> splits
    ) {
        return new RunningSessionDetailResponse(
                runningSession.getId(),
                runningSession.getUser().getId(),
                runningSession.getSnapshotImageUrl(),
                runningSession.getStartedAt(),
                runningSession.getEndedAt(),
                runningSession.getDistanceM(),
                runningSession.getDurationSec(),
                runningSession.getAvgSpeedMps(),
                runningSession.getAvgPaceSecPerKm(),
                runningSession.getCaloriesKcal(),
                runningSession.getElevationGainM(),
                gpsSamples,
                healthSamples,
                splits
        );
    }
}
