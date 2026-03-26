// OAuth 로그인 성공 시 호출됨.
// OAuth2User에서 provider 정보 읽음
// 1. 기존 계정 조회
// 2. 신규 사용자 생성
// 3. 그 다음 JWT발급/redirect로 넘길 준비

package kr.withrun.was.domain.auth.oauth2.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.withrun.was.domain.auth.dto.AuthTokenPair;
import kr.withrun.was.domain.auth.jwt.JwtProvider;
import kr.withrun.was.domain.auth.oauth2.userinfo.OAuth2UserInfo;
import kr.withrun.was.domain.auth.oauth2.userinfo.OAuth2UserInfoFactory;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.entity.UserAuthAccount;
import kr.withrun.was.domain.user.repository.UserAuthAccountRepository;
import kr.withrun.was.domain.user.repository.UserRepository;
import kr.withrun.was.global.config.AuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private static final String OAUTH_TARGET_COOKIE_NAME = "withrun_oauth_target";

    private final UserRepository userRepository;
    private final UserAuthAccountRepository userAuthAccountRepository;
    private final AuthProperties authProperties;
    // OAuth 로그인 성공 후 우리 서비스용 JWT를 발급하는 담당자
    private final JwtProvider jwtProvider;

    // Override
    // 부모 인터페이스나 부모 클래스에 이미 있는 메서드를 내가 다시 구현한 것.
    // Transactional
    // 이 메서드 안에서 DB 작업을 하나의 트랜잭션으로 묶어라.
    // 중간에 하나라도 실패하면 rollback을 의미
    @Override
    @Transactional
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        // OAuth 로그인 성공 후 Spring Security가 넘겨준 인증 객체를 꺼냅니다.
        OAuth2AuthenticationToken oAuth2AuthenticationToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // 현재 로그인한 소셜 제공자 이름(google, kakao)을 가져옵니다.
        String registrationId = oAuth2AuthenticationToken.getAuthorizedClientRegistrationId();

        // provider별 raw 응답을 공통 형식으로 파싱합니다.
        OAuth2UserInfo userInfo = OAuth2UserInfoFactory.of(registrationId, oAuth2User.getAttributes());

        User user;

        // provider + providerUserId로 기존 소셜 계정을 먼저 찾습니다.
        UserAuthAccount authAccount = userAuthAccountRepository
                .findByProviderAndProviderUserId(userInfo.getProvider(), userInfo.getProviderUserId())
                .orElse(null);

        if (authAccount != null) {
            // 이미 가입된 계정이면 연결된 기존 User를 사용합니다.
            user = authAccount.getUser();
        } else {
            // 처음 로그인한 계정이면 온보딩 전 상태의 User를 먼저 생성합니다.
            user = userRepository.save(
                    User.createPendingSocialUser(buildTemporaryNickname(
                            userInfo.getProvider(),
                            userInfo.getProviderUserId()
                    ))
            );

            // 생성한 User와 소셜 계정을 연결하는 매핑 정보를 저장합니다.
            UserAuthAccount newAuthAccount = UserAuthAccount.create(
                    user,
                    userInfo.getProvider(),
                    userInfo.getProviderUserId(),
                    userInfo.getEmail(),
                    userInfo.getProfileImageUrl()
            );
            userAuthAccountRepository.save(newAuthAccount);

        }

        boolean mobileLoginRequest = isMobileLoginRequest(request);

        if (mobileLoginRequest && StringUtils.hasText(authProperties.getMobileDeepLinkBase())) {
            // 모바일 분기는 외부 브라우저 쿠키 저장소를 믿지 않고, 짧은 교환 코드만 앱으로 넘긴다.
            response.addHeader(HttpHeaders.SET_COOKIE, clearOAuthTargetCookie().toString());
            String mobileAuthCode = jwtProvider.generateMobileAuthCode(user, userInfo.getProvider());
            response.sendRedirect(resolveMobileCallbackRedirectUrl(mobileAuthCode));
            return;
        }

        // 웹 로그인은 기존처럼 refresh cookie 기반으로 callback 화면이 세션을 복구한다.
        AuthTokenPair authTokenPair = jwtProvider.generateTokenPair(user, userInfo.getProvider());
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                jwtProvider.createRefreshTokenCookie(authTokenPair.refreshToken()).toString()
        );
        response.addHeader(HttpHeaders.SET_COOKIE, clearOAuthTargetCookie().toString());
        response.sendRedirect(resolveWebCallbackRedirectUrl());
    }

    private String resolveMobileCallbackRedirectUrl(String code) {
        return UriComponentsBuilder
                .fromUriString(authProperties.getMobileDeepLinkBase())
                .queryParam("code", code)
                .build(true)
                .toUriString();
    }

    private String resolveWebCallbackRedirectUrl() {
        return UriComponentsBuilder
                .fromUriString(authProperties.getFrontendBaseUrl())
                .path("/auth/callback")
                .build(true)
                .toUriString();
    }

    private boolean isMobileLoginRequest(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();

        if (cookies == null) {
            return false;
        }

        return Arrays.stream(cookies)
                .anyMatch(cookie ->
                        OAUTH_TARGET_COOKIE_NAME.equals(cookie.getName()) && "mobile".equals(cookie.getValue())
                );
    }

    private ResponseCookie clearOAuthTargetCookie() {
        return ResponseCookie.from(OAUTH_TARGET_COOKIE_NAME, "")
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
    }

    private String buildTemporaryNickname(String provider, String providerUserId) {
        // pending user 저장 시 닉네임 unique/length 제약에 걸리지 않도록 provider 식별자를 기반으로 임시 닉네임을 만듭니다.
        String normalizedProvider = provider == null
                ? "social"
                : provider.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        String normalizedProviderUserId = providerUserId == null
                ? "user"
                : providerUserId.replaceAll("[^a-zA-Z0-9]", "");

        String candidate = "pending-" + normalizedProvider + "-" + normalizedProviderUserId;
        return candidate.length() <= 40 ? candidate : candidate.substring(0, 40);
    }
}
