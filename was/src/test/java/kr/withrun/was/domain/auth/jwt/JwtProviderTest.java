package kr.withrun.was.domain.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import kr.withrun.was.domain.auth.dto.AuthTokenPair;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.type.Gender;
import kr.withrun.was.global.config.JwtProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtProvider")
class JwtProviderTest {

    private static final String ACCESS_SECRET = "0123456789abcdef0123456789abcdef";

    @Test
    @DisplayName("generates access and refresh tokens with expected claims")
    void generatesAccessAndRefreshTokensWithExpectedClaims() {
        JwtProvider jwtProvider = new JwtProvider(jwtProperties());
        User user = completedUser(10L);

        AuthTokenPair tokenPair = jwtProvider.generateTokenPair(user, "google");

        Claims accessClaims = parseClaims(tokenPair.accessToken());
        Claims refreshClaims = parseClaims(tokenPair.refreshToken());

        assertThat(accessClaims.getSubject()).isEqualTo("10");
        assertThat(accessClaims.get("userId", Long.class)).isEqualTo(10L);
        assertThat(accessClaims.get("provider", String.class)).isEqualTo("google");
        assertThat(accessClaims.get("profileCompleted", Boolean.class)).isTrue();
        assertThat(accessClaims.get("tokenType", String.class)).isEqualTo("access");

        assertThat(refreshClaims.getSubject()).isEqualTo("10");
        assertThat(refreshClaims.get("userId", Long.class)).isEqualTo(10L);
        assertThat(refreshClaims.get("tokenType", String.class)).isEqualTo("refresh");
    }

    @Test
    @DisplayName("creates refresh token cookie with configured attributes")
    void createsRefreshTokenCookieWithConfiguredAttributes() {
        JwtProvider jwtProvider = new JwtProvider(jwtProperties());

        ResponseCookie cookie = jwtProvider.createRefreshTokenCookie("refresh-token");

        assertThat(cookie.getName()).isEqualTo("refresh_token");
        assertThat(cookie.getValue()).isEqualTo("refresh-token");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.isSecure()).isFalse();
        assertThat(cookie.getPath()).isEqualTo("/");
        assertThat(cookie.getSameSite()).isEqualTo("Lax");
        assertThat(cookie.getMaxAge().getSeconds()).isEqualTo(1209600L);
    }

    @Test
    @DisplayName("throws when jwt secret is missing")
    void throwsWhenJwtSecretIsMissing() {
        JwtProperties jwtProperties = jwtProperties();
        jwtProperties.setAccessSecret(" ");
        JwtProvider jwtProvider = new JwtProvider(jwtProperties);

        assertThatThrownBy(() -> jwtProvider.generateTokenPair(completedUser(10L), "google"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("auth.jwt.access-secret is not configured");
    }

    private JwtProperties jwtProperties() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setAccessSecret(ACCESS_SECRET);
        jwtProperties.setAccessExpirationSeconds(3600L);
        jwtProperties.setRefreshExpirationSeconds(1209600L);
        jwtProperties.setRefreshCookieName("refresh_token");
        jwtProperties.setRefreshCookiePath("/");
        jwtProperties.setRefreshCookieSameSite("Lax");
        jwtProperties.setRefreshCookieSecure(false);
        return jwtProperties;
    }

    private Claims parseClaims(String token) {
        SecretKey secretKey = Keys.hmacShaKeyFor(ACCESS_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private User completedUser(Long id) {
        User user = User.createPendingSocialUser();
        setField(user, "id", id);
        user.completeProfile(
                "runner",
                LocalDate.of(1999, 1, 2),
                Gender.MALE,
                180.0,
                72.5
        );
        return user;
    }

    private void setField(Object target, String fieldName, Object value) {
        Class<?> currentClass = target.getClass();
        while (currentClass != null) {
            try {
                Field field = currentClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException exception) {
                currentClass = currentClass.getSuperclass();
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException("Failed to set field " + fieldName, exception);
            }
        }

        throw new IllegalArgumentException("Field not found: " + fieldName);
    }
}
