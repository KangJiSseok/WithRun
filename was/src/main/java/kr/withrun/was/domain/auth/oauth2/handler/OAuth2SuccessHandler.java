// OAuth 로그인 성공 시 호출됨.
// OAuth2User에서 provider 정보 읽음
// 1. 기존 계정 조회
// 2. 신규 사용자 생성
// 3. 그 다음 JWT발급/redirect로 넘길 준비

package kr.withrun.was.domain.auth.oauth2.handler;

import jakarta.servlet.ServletException;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

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
        boolean isNewUser = false;

        // provider + providerUserId로 기존 소셜 계정을 먼저 찾습니다.
        UserAuthAccount authAccount = userAuthAccountRepository
                .findByProviderAndProviderUserId(userInfo.getProvider(), userInfo.getProviderUserId())
                .orElse(null);

        if (authAccount != null) {
            // 이미 가입된 계정이면 연결된 기존 User를 사용합니다.
            user = authAccount.getUser();
        } else {
            // 처음 로그인한 계정이면 온보딩 전 상태의 User를 먼저 생성합니다.
            user = userRepository.save(User.createPendingSocialUser());

            // 생성한 User와 소셜 계정을 연결하는 매핑 정보를 저장합니다.
            UserAuthAccount newAuthAccount = UserAuthAccount.create(
                    user,
                    userInfo.getProvider(),
                    userInfo.getProviderUserId(),
                    userInfo.getEmail(),
                    userInfo.getProfileImageUrl()
            );
            userAuthAccountRepository.save(newAuthAccount);

            isNewUser = true;
        }

        // 로그인 성공 후 access token / refresh token을 발급합니다.
        AuthTokenPair authTokenPair = jwtProvider.generateTokenPair(user, userInfo.getProvider());

        // refresh token은 브라우저에서 직접 읽지 못하도록 HttpOnly 쿠키로 내려줍니다.
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                jwtProvider.createRefreshTokenCookie(authTokenPair.refreshToken()).toString()
        );

        // access token은 우선 callback 쿼리로 전달하고, 프론트는 이 값을 받아 후속 API 호출에 사용합니다.
        // 이후 /api/auth/me, /reissue 흐름이 붙으면 callback 쿼리 의존성은 줄일 수 있습니다.
        String redirectUrl = UriComponentsBuilder
                .fromUriString(authProperties.getFrontendBaseUrl())
                .path("/auth/callback")
                .queryParam("accessToken", authTokenPair.accessToken())
                .queryParam("provider", userInfo.getProvider())
                .queryParam("isNewUser", isNewUser)
                .queryParam("profileCompleted", user.isProfileCompleted())
                .build(true)
                .toUriString();

        response.sendRedirect(redirectUrl);
    }
}
