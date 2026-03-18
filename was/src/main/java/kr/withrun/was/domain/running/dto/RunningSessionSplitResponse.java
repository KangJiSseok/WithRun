package kr.withrun.was.domain.running.dto;

import kr.withrun.was.domain.running.entity.RunningSessionSplit;

public record RunningSessionSplitResponse(
        Integer splitIndex,
        Integer splitDistanceM,
        Integer splitDurationSec,
        Integer splitPaceSecPerKm,
        Integer avgHeartRate,
        Integer elevationGainM
) {
    public static RunningSessionSplitResponse from(RunningSessionSplit runningSessionSplit) {
        return new RunningSessionSplitResponse(
                runningSessionSplit.getSplitIndex(),
                runningSessionSplit.getSplitDistanceM(),
                runningSessionSplit.getSplitDurationSec(),
                runningSessionSplit.getSplitPaceSecPerKm(),
                runningSessionSplit.getAvgHeartRate(),
                runningSessionSplit.getElevationGainM()
        );
    }
}
