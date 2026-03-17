package kr.withrun.was.domain.running.dto;

public record CreateRunningGpsSampleBatchResponse(
        Long runningSessionId,
        Long userId,
        int savedCount
) {

    public static CreateRunningGpsSampleBatchResponse of(
            Long runningSessionId,
            Long userId,
            int savedCount
    ) {
        return new CreateRunningGpsSampleBatchResponse(runningSessionId, userId, savedCount);
    }
}
