package kr.withrun.was.domain.user.controller;

import jakarta.validation.Valid;
import kr.withrun.was.domain.auth.security.AuthenticatedUser;
import kr.withrun.was.domain.user.dto.UpdateUserProfileRequest;
import kr.withrun.was.domain.user.dto.UserProfileResponse;
import kr.withrun.was.domain.user.service.UserProfileService;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ApiResponse;
import kr.withrun.was.global.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// "현재 로그인한 사용자"의 프로필만 수정하는 엔드포인트입니다.
// userId를 외부에서 받지 않고 SecurityContext의 principal에서 꺼내는 것이 핵심입니다.
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        // 마이페이지 진입 시 현재 로그인 사용자의 프로필 기본값을 복구하는 용도입니다.
        return ApiResponse.successEntity(
                userProfileService.getProfile(resolveUserId(authenticatedUser))
        );
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<Void>> updateProfile(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpdateUserProfileRequest request
    ) {
        // 응답 바디에 사용자 정보를 다시 싣지 않고, 성공 여부만 반환합니다.
        // 프런트 상태 동기화는 다음 Step의 /api/auth/me에서 담당할 예정입니다.
        userProfileService.updateProfile(resolveUserId(authenticatedUser), request);
        return ApiResponse.successEntity(ResponseCode.OK);
    }

    private Long resolveUserId(AuthenticatedUser authenticatedUser) {
        // Security 설정상 authenticated() 요청이면 보통 principal이 있어야 하지만,
        // 방어적으로 한 번 더 확인해 컨트롤러 단에서도 명확한 401을 보장합니다.
        if (authenticatedUser == null || authenticatedUser.userId() == null) {
            throw new CustomException(ResponseCode.UNAUTHORIZED);
        }

        return authenticatedUser.userId();
    }
}
