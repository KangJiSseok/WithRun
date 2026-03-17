package kr.withrun.was.domain.running.dto;

import java.util.List;

public record PastRunningSessionsResponse(
        List<PastRunningSessionItemResponse> items,
        boolean hasMore,
        String nextCursor
) {
}
