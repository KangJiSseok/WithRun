package kr.withrun.was.domain.auth.oauth2.handler;

import kr.withrun.was.global.config.AuthProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OAuth2 로그인 실패 핸들러")
class OAuth2FailureHandlerTest {

    private final OAuth2FailureHandler failureHandler = new OAuth2FailureHandler(authProperties());

    @DisplayName("provider가 access_denied를 넘기면 로그인 화면으로 access_denied 에러를 전달한다")
    @Test
    void redirectsToLoginWithAccessDeniedWhenProviderReturnsAccessDenied() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setParameter("error", "access_denied");
        MockHttpServletResponse response = new MockHttpServletResponse();

        failureHandler.onAuthenticationFailure(
                request,
                response,
                new OAuth2AuthenticationException(new OAuth2Error("access_denied"))
        );

        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:5173/login?error=access_denied");
    }

    @DisplayName("OAuth authorization request가 사라지면 cancelled 에러로 리다이렉트한다")
    @Test
    void redirectsToLoginWithCancelledWhenAuthorizationRequestIsMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        failureHandler.onAuthenticationFailure(
                request,
                response,
                new OAuth2AuthenticationException(new OAuth2Error("authorization_request_not_found"))
        );

        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:5173/login?error=cancelled");
    }

    @DisplayName("기타 예외는 server_error로 리다이렉트한다")
    @Test
    void redirectsToLoginWithServerErrorForUnexpectedFailures() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        failureHandler.onAuthenticationFailure(
                request,
                response,
                new BadCredentialsException("unexpected")
        );

        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:5173/login?error=server_error");
    }

    private static AuthProperties authProperties() {
        AuthProperties authProperties = new AuthProperties();
        authProperties.setFrontendBaseUrl("http://localhost:5173");
        return authProperties;
    }
}
