package kr.withrun.was.domain.auth.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import kr.withrun.was.domain.auth.dto.AuthTokenPair;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.global.config.JwtProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final JwtProperties jwtProperties;

    // 로그인 성공 후 access token / refresh token을 한 번에 발급합니다.
    public AuthTokenPair generateTokenPair(User user, String provider) {
        return new AuthTokenPair(
                generateAccessToken(user, provider),
                generateRefreshToken(user)
        );
    }

    // refresh token은 브라우저 자바스크립트에서 직접 읽지 못하도록 HttpOnly 쿠키로 만듭니다.
    public ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from(jwtProperties.getRefreshCookieName(), refreshToken)
                .httpOnly(true)
                .secure(jwtProperties.isRefreshCookieSecure())
                .sameSite(jwtProperties.getRefreshCookieSameSite())
                .path(jwtProperties.getRefreshCookiePath())
                .maxAge(jwtProperties.getRefreshExpirationSeconds())
                .build();
    }

    // access token에는 현재 사용자 식별 정보와 프론트 분기에 필요한 최소 클레임만 담습니다.
    private String generateAccessToken(User user, String provider) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("userId", user.getId())
                .claim("provider", provider)
                .claim("profileCompleted", user.isProfileCompleted())
                .claim("tokenType", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(jwtProperties.getAccessExpirationSeconds())))
                .signWith(getSigningKey())
                .compact();
    }

    // refresh token은 재발급 용도라서 사용자 식별과 tokenType 중심으로 단순하게 만듭니다.
    private String generateRefreshToken(User user) {
        Instant now = Instant.now();

        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("userId", user.getId())
                .claim("tokenType", "refresh")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(jwtProperties.getRefreshExpirationSeconds())))
                .signWith(getSigningKey())
                .compact();
    }

    // 설정값에서 읽은 secret으로 JJWT 서명 키를 생성합니다.
    private SecretKey getSigningKey() {
        String accessSecret = jwtProperties.getAccessSecret();
        if (accessSecret == null || accessSecret.isBlank()) {
            throw new IllegalStateException("auth.jwt.access-secret is not configured");
        }

        return Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
    }
}
