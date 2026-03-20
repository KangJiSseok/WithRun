package kr.withrun.was.domain.running.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import kr.withrun.was.domain.running.entity.RunningSession;

import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
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
        List<RunningSessionSplitResponse> splits,
        GhostRunningResultResponse ghostRunningResult
) {
    public static RunningSessionDetailResponse of(
            RunningSession runningSession,
            List<RunningGpsSampleResponse> gpsSamples,
            List<RunningHealthSampleResponse> healthSamples,
            List<RunningSessionSplitResponse> splits
    ) {
        return of(runningSession, gpsSamples, healthSamples, splits, runningSession.getSnapshotImageUrl(), null);
    }

    public static RunningSessionDetailResponse of(
            RunningSession runningSession,
            List<RunningGpsSampleResponse> gpsSamples,
            List<RunningHealthSampleResponse> healthSamples,
            List<RunningSessionSplitResponse> splits,
            GhostRunningResultResponse ghostRunningResult
    ) {
        return of(
                runningSession,
                gpsSamples,
                healthSamples,
                splits,
                runningSession.getSnapshotImageUrl(),
                ghostRunningResult
        );
    }

    public static RunningSessionDetailResponse of(
            RunningSession runningSession,
            List<RunningGpsSampleResponse> gpsSamples,
            List<RunningHealthSampleResponse> healthSamples,
            List<RunningSessionSplitResponse> splits,
            String snapshotImageUrl,
            GhostRunningResultResponse ghostRunningResult
    ) {
        return new RunningSessionDetailResponse(
                runningSession.getId(),
                runningSession.getUser().getId(),
                snapshotImageUrl,
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
                splits,
                ghostRunningResult
        );
    }
}
