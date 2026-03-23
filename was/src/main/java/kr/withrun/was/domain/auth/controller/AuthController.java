package kr.withrun.was.domain.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.withrun.was.domain.auth.dto.AuthMeResponse;
import kr.withrun.was.domain.auth.dto.ReissueAccessTokenResponse;
import kr.withrun.was.domain.auth.security.AuthenticatedUser;
import kr.withrun.was.domain.auth.service.AuthService;
import kr.withrun.was.global.response.ApiResponse;
import kr.withrun.was.global.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthMeResponse>> getCurrentUser(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        // 프론트가 앱 진입 시 "지금 누구로 로그인돼 있는지" 복구할 때 사용하는 엔드포인트입니다.
        return ApiResponse.successEntity(authService.getCurrentUser(authenticatedUser));
    }

    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<ReissueAccessTokenResponse>> reissueAccessToken(
            HttpServletRequest request
    ) {
        // access token이 만료됐을 때 refresh token 쿠키만으로 새 access token을 재발급합니다.
        return ApiResponse.successEntity(authService.reissueAccessToken(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        // 서버 세션을 직접 다루기보다 refresh token 쿠키를 만료시켜 로그아웃 상태로 돌립니다.
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                authService.expireRefreshTokenCookie().toString()
        );
        return ApiResponse.successEntity(ResponseCode.USER_LOGGED_OUT);
    }
}
