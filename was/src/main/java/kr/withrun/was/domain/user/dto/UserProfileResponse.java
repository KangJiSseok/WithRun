package kr.withrun.was.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.type.Gender;

import java.time.LocalDate;

@Schema(description = "내 프로필 조회 응답")
public record UserProfileResponse(
        @Schema(description = "닉네임", example = "runner", nullable = true)
        String nickname,

        @Schema(description = "생년월일", example = "1998-04-05", nullable = true)
        LocalDate birthDate,

        @Schema(description = "성별", implementation = Gender.class, nullable = true)
        Gender gender,

        @Schema(description = "키(cm)", example = "165.5", nullable = true)
        Double height,

        @Schema(description = "몸무게(kg)", example = "52.3", nullable = true)
        Double weight
) {
    public static UserProfileResponse from(User user) {
        if (!user.isProfileCompleted()) {
            return new UserProfileResponse(null, null, null, null, null);
        }

        return new UserProfileResponse(
                user.getNickname(),
                user.getBirthDate(),
                user.getGender(),
                user.getHeight(),
                user.getWeight()
        );
    }
}
