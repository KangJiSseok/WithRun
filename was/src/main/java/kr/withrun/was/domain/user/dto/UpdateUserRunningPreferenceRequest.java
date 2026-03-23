package kr.withrun.was.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.domain.user.type.Purpose;
import kr.withrun.was.global.common.type.Difficulty;
import kr.withrun.was.global.common.type.TimeSlot;

@Schema(description = "내 러닝 기본 설정 수정 요청")
public record UpdateUserRunningPreferenceRequest(
        @Schema(description = "러닝 목적", implementation = Purpose.class, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Purpose purpose,

        @Schema(description = "선호 시간대", implementation = TimeSlot.class, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        TimeSlot timeSlot,

        @Schema(description = "선호 거리(km)", example = "5.0", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        @Positive
        Double preferredDistanceKm,

        @Schema(description = "선호 난이도", implementation = Difficulty.class, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Difficulty preferredDifficulty,

        @Schema(description = "선호 코스 타입", implementation = CourseType.class, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        CourseType courseType
) {
}
