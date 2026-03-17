package kr.withrun.was.domain.running.dto;

import kr.withrun.was.domain.running.entity.RunningSessionSplit;

public record CreateRunningSessionSplitResponse(
        Long runningSessionSplitId,
        Long runningSessionId,
        Integer splitIndex,
        Integer splitDistanceM,
        Integer splitDurationSec,
        Integer splitPaceSecPerKm,
        Integer avgHeartRate,
        Integer elevationGainM
) {
    public static CreateRunningSessionSplitResponse from(RunningSessionSplit runningSessionSplit) {
        return new CreateRunningSessionSplitResponse(
                runningSessionSplit.getId(),
                runningSessionSplit.getRunningSession().getId(),
                runningSessionSplit.getSplitIndex(),
                runningSessionSplit.getSplitDistanceM(),
                runningSessionSplit.getSplitDurationSec(),
                runningSessionSplit.getSplitPaceSecPerKm(),
                runningSessionSplit.getAvgHeartRate(),
                runningSessionSplit.getElevationGainM()
        );
    }
}
