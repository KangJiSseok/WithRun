package kr.withrun.was.domain.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import kr.withrun.was.domain.auth.dto.AuthTokenPair;
import kr.withrun.was.domain.auth.security.AuthenticatedUser;
import kr.withrun.was.domain.auth.security.JwtAuthenticationException;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.global.config.JwtProperties;
import kr.withrun.was.global.response.ResponseCode;
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

    public Claims parseAccessToken(String accessToken) {
        // 서명, 만료, 형식 검증은 JJWT parser가 수행합니다.
        // 여기서는 그 위에 한 단계 더 얹어서 "정말 access token이 맞는지"를 tokenType으로 확인합니다.
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(accessToken)
                .getPayload();

        String tokenType = claims.get("tokenType", String.class);
        if (!"access".equals(tokenType)) {
            throw new JwtAuthenticationException(ResponseCode.TOKEN_INVALID, "access token is required");
        }

        return claims;
    }

    public AuthenticatedUser extractAuthenticatedUser(String accessToken) {
        Claims claims = parseAccessToken(accessToken);
        // 이후 서비스 계층에서 사용자 조회의 기준이 되는 userId claim을 꺼냅니다.
        Long userId = claims.get("userId", Long.class);
        if (userId == null) {
            throw new JwtAuthenticationException(ResponseCode.TOKEN_INVALID, "userId claim is missing");
        }

        return new AuthenticatedUser(userId);
    }

    // access token에는 현재 사용자 식별 정보와 프론트 분기에 필요한 최소 클레임만 담습니다.
    private String generateAccessToken(User user, String provider) {
        Instant now = Instant.now();

        // 프런트가 즉시 필요한 정보만 최소한으로 담습니다.
        // provider와 profileCompleted는 다음 단계에서 화면 분기나 상태 복구에 활용할 수 있습니다.
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

        // refresh token은 재발급 용도이므로 provider/profileCompleted 같은 화면용 정보는 넣지 않습니다.
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
