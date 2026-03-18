package kr.withrun.was.domain.course.dto;

import java.time.LocalDateTime;

public record CourseLeaderboardItemResponse(
        Long leaderboardId,
        Long userId,
        String nickname,
        Long runningSessionId,
        Integer point,
        Long rank,
        LocalDateTime createdAt
) {
}
