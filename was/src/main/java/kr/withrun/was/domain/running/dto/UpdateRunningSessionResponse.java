package kr.withrun.was.domain.running.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.withrun.was.domain.running.entity.RunningSession;

import java.time.LocalDateTime;

@Schema(description = "러닝 세션 종료 결과")
public record UpdateRunningSessionResponse(
        @Schema(description = "러닝 세션 ID", example = "91")
        Long runningSessionId,

        @Schema(description = "러닝 완료 여부", example = "true")
        boolean isCompleted,

        @Schema(description = "외부 공개 여부", example = "false")
        boolean isPublic,

        @Schema(description = "러닝 시작 시각", example = "2026-03-17T06:30:00")
        LocalDateTime startedAt,

        @Schema(description = "러닝 종료 시각", example = "2026-03-17T07:02:15")
        LocalDateTime endedAt,

        @Schema(description = "최종 러닝 거리(m)", example = "5320")
        int distanceM,

        @Schema(description = "최종 소모 칼로리(kcal)", example = "328")
        int caloriesKcal,

        @Schema(description = "러닝 종료 지점 위도", example = "37.5701", nullable = true)
        Double endLatitude,

        @Schema(description = "러닝 종료 지점 경도", example = "126.9812", nullable = true)
        Double endLongitude,

        @Schema(description = "평균 속도(m/s)", example = "2.92", nullable = true)
        Double avgSpeedMps,

        @Schema(description = "총 러닝 시간(초)", example = "1925", nullable = true)
        Integer durationSec,

        @Schema(description = "평균 페이스(sec/km)", example = "362", nullable = true)
        Integer avgPaceSecPerKm,

        @Schema(description = "러닝 시작 지점 위도", example = "37.5665")
        double startLatitude,

        @Schema(description = "러닝 시작 지점 경도", example = "126.9780")
        double startLongitude,

        @Schema(description = "누적 상승고도(m)", example = "46")
        int elevationGainM
) {

    public static UpdateRunningSessionResponse from(RunningSession runningSession) {
        return new UpdateRunningSessionResponse(
                runningSession.getId(),
                runningSession.getIsCompleted(),
                runningSession.getIsPublic(),
                runningSession.getStartedAt(),
                runningSession.getEndedAt(),
                runningSession.getDistanceM(),
                runningSession.getCaloriesKcal(),
                runningSession.getEndLatitude(),
                runningSession.getEndLongitude(),
                runningSession.getAvgSpeedMps(),
                runningSession.getDurationSec(),
                runningSession.getAvgPaceSecPerKm(),
                runningSession.getStartLatitude(),
                runningSession.getStartLongitude(),
                runningSession.getElevationGainM()
        );
    }
}
