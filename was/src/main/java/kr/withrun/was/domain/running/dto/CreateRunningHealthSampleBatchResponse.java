package kr.withrun.was.domain.running.dto;

public record CreateRunningHealthSampleBatchResponse(
        Long sessionId,
        Long userId,
        int savedCount
) {
    public static CreateRunningHealthSampleBatchResponse of(
            Long runningSessionId,
            Long userId,
            int savedCount
    ) {
        return new CreateRunningHealthSampleBatchResponse(runningSessionId, userId, savedCount);
    }
}
