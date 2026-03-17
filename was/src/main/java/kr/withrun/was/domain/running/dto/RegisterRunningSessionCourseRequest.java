package kr.withrun.was.domain.running.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record RegisterRunningSessionCourseRequest(
        @NotBlank
        String title,

        @NotBlank
        String difficulty,

        @NotEmpty
        List<@NotBlank String> courseTypes,

        String snapshotImageUrl
) {
}
