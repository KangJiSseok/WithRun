package kr.withrun.was.domain.auth.service;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import kr.withrun.was.domain.auth.dto.AuthMeResponse;
import kr.withrun.was.domain.auth.dto.ReissueAccessTokenResponse;
import kr.withrun.was.domain.auth.jwt.JwtProvider;
import kr.withrun.was.domain.auth.security.AuthenticatedUser;
import kr.withrun.was.domain.auth.security.JwtAuthenticationException;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.entity.UserAuthAccount;
import kr.withrun.was.domain.user.repository.UserAuthAccountRepository;
import kr.withrun.was.domain.user.repository.UserRepository;
import kr.withrun.was.global.config.JwtProperties;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserAuthAccountRepository userAuthAccountRepository;
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;

    public AuthMeResponse getCurrentUser(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.userId() == null) {
            throw new CustomException(ResponseCode.UNAUTHORIZED);
        }

        User user = userRepository.findNotDeletedUser(authenticatedUser.userId())
                .orElseThrow(() -> new CustomException(ResponseCode.USER_NOT_FOUND));

        return AuthMeResponse.from(user, authenticatedUser);
    }

    public ReissueAccessTokenResponse reissueAccessToken(HttpServletRequest request) {
        String refreshToken = extractRefreshToken(request);
        Long userId = extractUserIdFromRefreshToken(refreshToken);

        User user = userRepository.findNotDeletedUser(userId)
                .orElseThrow(() -> new CustomException(ResponseCode.USER_NOT_FOUND));

        String provider = userAuthAccountRepository.findById(userId)
                .map(UserAuthAccount::getProvider)
                .orElseThrow(() -> new CustomException(ResponseCode.UNAUTHORIZED));

        String accessToken = jwtProvider.generateAccessToken(user, provider);
        return new ReissueAccessTokenResponse(accessToken, provider, user.isProfileCompleted());
    }

    public ResponseCookie expireRefreshTokenCookie() {
        return jwtProvider.expireRefreshTokenCookie();
    }

    private String extractRefreshToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            throw new CustomException(ResponseCode.UNAUTHORIZED);
        }

        return Arrays.stream(cookies)
                .filter(cookie -> jwtProperties.getRefreshCookieName().equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElseThrow(() -> new CustomException(ResponseCode.UNAUTHORIZED));
    }

    private Long extractUserIdFromRefreshToken(String refreshToken) {
        try {
            return jwtProvider.extractUserIdFromRefreshToken(refreshToken);
        } catch (ExpiredJwtException exception) {
            throw new CustomException(ResponseCode.TOKEN_EXPIRED);
        } catch (JwtAuthenticationException exception) {
            throw new CustomException(exception.getResponseCode());
        } catch (JwtException | IllegalArgumentException exception) {
            throw new CustomException(ResponseCode.TOKEN_INVALID);
        }
    }
}
