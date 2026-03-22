package kr.withrun.was.domain.running.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

@Schema(description = "완료한 러닝 세션을 코스로 등록할 때 사용하는 요청")
public record RegisterRunningSessionCourseRequest(
        @NotBlank
        @Schema(description = "생성할 코스 제목", example = "한강 야간 러닝 10K", requiredMode = Schema.RequiredMode.REQUIRED)
        String title,

        @NotBlank
        @Schema(description = "코스 공개 범위 enum 이름. COMMUNITY 또는 PRIVATE 만 허용된다.", example = "COMMUNITY", requiredMode = Schema.RequiredMode.REQUIRED)
        String mode,

        @NotBlank
        @Schema(description = "체감 난이도 enum 이름", example = "MEDIUM", requiredMode = Schema.RequiredMode.REQUIRED)
        String difficulty,

        @NotEmpty
        @Schema(description = "코스 유형 enum 이름 목록. 중복 값은 서버에서 제거된다.", example = "[\"RIVERSIDE\",\"URBAN\"]", requiredMode = Schema.RequiredMode.REQUIRED)
        List<@NotBlank String> courseTypes,

        @Schema(description = "코스 스냅샷 이미지 URL. 비우면 러닝 세션의 스냅샷을 재사용한다.", example = "https://cdn.example.com/course/snapshot.png", nullable = true)
        String snapshotImageUrl
) {
}
