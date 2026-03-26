package kr.withrun.was.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record AuthCodeExchangeRequest(
        @NotBlank(message = "code is required")
        String code
) {
}
