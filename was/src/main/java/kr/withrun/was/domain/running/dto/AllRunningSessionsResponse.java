package kr.withrun.was.domain.running.dto;

import java.util.List;

public record AllRunningSessionsResponse(
        List<AllRunningSessionItemResponse> items,
        boolean hasMore,
        String nextCursor
) {
}
