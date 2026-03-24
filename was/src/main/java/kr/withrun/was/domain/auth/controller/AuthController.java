package kr.withrun.was.domain.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.withrun.was.domain.auth.dto.AuthMeResponse;
import kr.withrun.was.domain.auth.dto.ReissueAccessTokenResponse;
import kr.withrun.was.domain.auth.security.AuthenticatedUser;
import kr.withrun.was.domain.auth.service.AuthService;
import kr.withrun.was.global.response.ApiResponse;
import kr.withrun.was.global.response.ResponseCode;
import kr.withrun.was.global.response.swagger.ErrorApiResponseDoc;
import kr.withrun.was.global.response.swagger.SuccessApiResponseDocs;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증 상태 조회, access token 재발급, 로그아웃 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(
            summary = "현재 로그인 사용자를 조회한다",
            description = "access token 기준으로 현재 로그인된 사용자 정보와 프로필 완료 여부를 조회한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "현재 사용자 조회 성공",
                    content = @Content(schema = @Schema(implementation = SuccessApiResponseDocs.AuthMeApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증이 없거나 access token 이 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            )
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthMeResponse>> getCurrentUser(
            @Parameter(hidden = true)
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ApiResponse.successEntity(authService.getCurrentUser(authenticatedUser));
    }

    @Operation(
            summary = "access token 을 재발급한다",
            description = "refresh token 쿠키를 사용해 새로운 access token 을 재발급한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "access token 재발급 성공",
                    content = @Content(schema = @Schema(implementation = SuccessApiResponseDocs.ReissueAccessTokenApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "refresh token 이 없거나 유효하지 않음",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "사용자를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ErrorApiResponseDoc.class))
            )
    })
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<ReissueAccessTokenResponse>> reissueAccessToken(
            @Parameter(hidden = true) HttpServletRequest request
    ) {
        return ApiResponse.successEntity(authService.reissueAccessToken(request));
    }

    @Operation(
            summary = "로그아웃한다",
            description = "refresh token 쿠키를 만료시켜 로그아웃 상태로 전환한다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그아웃 성공",
                    content = @Content(schema = @Schema(implementation = SuccessApiResponseDocs.LogoutApiResponse.class))
            )
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Parameter(hidden = true) HttpServletResponse response
    ) {
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                authService.expireRefreshTokenCookie().toString()
        );
        return ApiResponse.successEntity(ResponseCode.USER_LOGGED_OUT);
    }
}
