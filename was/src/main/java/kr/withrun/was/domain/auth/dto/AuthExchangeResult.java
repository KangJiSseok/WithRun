package kr.withrun.was.domain.auth.dto;

import org.springframework.http.ResponseCookie;

public record AuthExchangeResult(
        ReissueAccessTokenResponse response,
        ResponseCookie refreshTokenCookie
) {
}
