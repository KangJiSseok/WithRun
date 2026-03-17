package kr.withrun.was.domain.running.dto;

import kr.withrun.was.domain.running.repository.query.dto.PastRunningSessionHistoryRow;
import kr.withrun.was.domain.running.type.GhostResultStatus;
import kr.withrun.was.domain.running.type.RunningMode;
import kr.withrun.was.global.common.type.TimeSlot;

import java.time.LocalDateTime;

public record PastRunningSessionItemResponse(
        Long runningSessionId,
        LocalDateTime startedAt,
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

    public static PastRunningSessionItemResponse from(PastRunningSessionHistoryRow row) {
        return new PastRunningSessionItemResponse(
                row.runningSessionId(),
                row.startedAt(),
                row.distanceM(),
                row.durationSec(),
                row.caloriesKcal(),
                row.snapshotImageUrl(),
                row.elevationGainM(),
                Boolean.TRUE.equals(row.isCompleted()),
                row.runningMode(),
                row.ghostResultStatus(),
                TimeSlot.from(row.startedAt().toLocalTime())
        );
    }
}
