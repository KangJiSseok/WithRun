package kr.withrun.was.domain.running.repository.query.dto;

import kr.withrun.was.domain.running.type.GhostResultStatus;
import kr.withrun.was.domain.running.type.RunningMode;

import java.time.LocalDateTime;

public record RunningSessionHistoryRow(
        Long runningSessionId,
        LocalDateTime startedAt,
        Integer distanceM,
        Integer durationSec,
        Integer caloriesKcal,
        String snapshotImageUrl,
        Integer elevationGainM,
        Boolean isCompleted,
        RunningMode runningMode,
        GhostResultStatus ghostResultStatus
) {
}
