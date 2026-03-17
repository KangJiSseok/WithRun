package kr.withrun.was.domain.running.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.withrun.was.domain.running.entity.RunningSession;
import kr.withrun.was.domain.running.type.RunningMode;

@Schema(description = "러닝 세션 시작 결과")
public record CreateRunningSessionResponse(
        @Schema(description = "생성된 러닝 세션 ID", example = "91")
        Long runningSessionId,

        @Schema(description = "생성된 세션의 러닝 모드")
        RunningMode mode,

        @Schema(description = "연결된 코스 ID. 자유 러닝이면 null 이다.", example = "12", nullable = true)
        Long courseId,

        @Schema(description = "목표 고스트 세션 ID. 고스트 러닝이 아니면 null 이다.", example = "77", nullable = true)
        Long ghostTargetRunningSessionId,

        @Schema(description = "현재 누적 거리(m). 시작 직후에는 0 이다.", example = "0")
        int distanceM,

        @Schema(description = "현재 누적 칼로리(kcal). 시작 직후에는 0 이다.", example = "0")
        int caloriesKcal,

        @Schema(description = "러닝 완료 여부. 세션 생성 직후에는 false 다.", example = "false")
        boolean isCompleted
) {

    public static CreateRunningSessionResponse from(RunningSession runningSession) {
        return new CreateRunningSessionResponse(
                runningSession.getId(),
                runningSession.getMode(),
                runningSession.getCourse() != null
                        ? runningSession.getCourse().getId()
                        : null,
                runningSession.getGhostTargetRunningSession() != null
                        ? runningSession.getGhostTargetRunningSession().getId()
                        : null,
                runningSession.getDistanceM(),
                runningSession.getCaloriesKcal(),
                Boolean.TRUE.equals(runningSession.getIsCompleted())
        );
    }
}
