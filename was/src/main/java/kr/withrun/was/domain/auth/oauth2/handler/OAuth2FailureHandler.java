package kr.withrun.was.domain.auth.oauth2.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.withrun.was.global.config.AuthProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class OAuth2FailureHandler implements AuthenticationFailureHandler {

    private final AuthProperties authProperties;

    @Override
    public void onAuthenticationFailure(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception
    ) throws IOException, ServletException {
        String error = resolveError(request, exception);
        // 실패 시 프론트 /login 으로 리다이렉트
        String redirectUrl = UriComponentsBuilder
                .fromUriString(authProperties.getFrontendBaseUrl())
                .path("/login")
                .queryParam("error", error)
                .build(true)
                .toUriString();

        response.sendRedirect(redirectUrl);
    }

    private String resolveError(HttpServletRequest request, AuthenticationException exception) {
        // provider가 쿼리 파라미터로 직접 내려준 오류가 있으면 가장 우선으로 사용합니다.
        String requestError = request.getParameter("error");
        if ("access_denied".equals(requestError)) {
            return "access_denied";
        }

        if (exception instanceof OAuth2AuthenticationException oAuth2Exception) {
            String errorCode = oAuth2Exception.getError().getErrorCode();

            if ("access_denied".equals(errorCode)) {
                return "access_denied";
            }

            // 사용자가 로그인 도중 취소했거나, OAuth state/session이 끊긴 경우를 취소 계열로 묶습니다.
            if ("authorization_request_not_found".equals(errorCode)
                    || "invalid_state_parameter".equals(errorCode)
                    || "invalid_request".equals(errorCode)) {
                return "cancelled";
            }
        }

        return "server_error";
    }
}
