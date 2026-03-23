package kr.withrun.was.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.domain.user.entity.UserPreferenceCourseType;
import kr.withrun.was.domain.user.entity.UserPreferencePurpose;
import kr.withrun.was.domain.user.entity.UserPreferenceTimeSlot;
import kr.withrun.was.domain.user.entity.UserRunningPreference;
import kr.withrun.was.domain.user.type.Purpose;
import kr.withrun.was.global.common.type.Difficulty;
import kr.withrun.was.global.common.type.TimeSlot;

@Schema(description = "내 러닝 기본 설정 조회 응답")
public record UserRunningPreferenceResponse(
        @Schema(description = "러닝 목적", implementation = Purpose.class, nullable = true)
        Purpose purpose,

        @Schema(description = "선호 시간대", implementation = TimeSlot.class, nullable = true)
        TimeSlot timeSlot,

        @Schema(description = "선호 거리(km)", example = "5.0", nullable = true)
        Double preferredDistanceKm,

        @Schema(description = "선호 난이도", implementation = Difficulty.class, nullable = true)
        Difficulty preferredDifficulty,

        @Schema(description = "선호 코스 타입", implementation = CourseType.class, nullable = true)
        CourseType courseType
) {
    public static UserRunningPreferenceResponse empty() {
        // 아직 저장된 기본 설정이 없는 초기 사용자를 위한 기본 응답입니다.
        return new UserRunningPreferenceResponse(null, null, null, null, null);
    }

    public static UserRunningPreferenceResponse from(UserRunningPreference preference) {
        if (preference == null) {
            return empty();
        }

        // 현재 제품 요구사항은 단일 선택이므로 각 컬렉션에서 첫 번째 값만 응답으로 노출합니다.
        return new UserRunningPreferenceResponse(
                preference.getPurposes().stream()
                        .map(UserPreferencePurpose::getPurpose)
                        .findFirst()
                        .orElse(null),
                preference.getTimeSlots().stream()
                        .map(UserPreferenceTimeSlot::getTimeSlot)
                        .findFirst()
                        .orElse(null),
                preference.getPreferredDistanceKm(),
                preference.getPreferredDifficulty(),
                preference.getCourseTypes().stream()
                        .map(UserPreferenceCourseType::getCourseType)
                        .findFirst()
                        .orElse(null)
        );
    }
}
