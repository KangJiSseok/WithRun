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
        return ApiResponse.successEntity(authService.getCurrentUser(authenticatedUser));
    }

    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<ReissueAccessTokenResponse>> reissueAccessToken(
            HttpServletRequest request
    ) {
        return ApiResponse.successEntity(authService.reissueAccessToken(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                authService.expireRefreshTokenCookie().toString()
        );
        return ApiResponse.successEntity(ResponseCode.USER_LOGGED_OUT);
    }
}
