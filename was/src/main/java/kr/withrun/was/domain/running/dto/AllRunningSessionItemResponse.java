package kr.withrun.was.domain.running.dto;

import kr.withrun.was.domain.running.type.GhostResultStatus;
import kr.withrun.was.domain.running.type.RunningMode;
import kr.withrun.was.domain.user.type.TimeSlot;

public record AllRunningSessionItemResponse(
        Long runningSessionId,
        Integer distanceM,
        Integer durationSec,
        Integer caloriesKcal,
        String snapshotImageUrl,
        Integer elevationGainM,
        boolean isCompleted,
        RunningMode runningMode,
        GhostResultStatus ghostResultStatus,
        TimeSlot timeSlot
) {
}
