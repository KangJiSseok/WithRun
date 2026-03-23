package kr.withrun.was.domain.user.controller;

import jakarta.validation.Valid;
import kr.withrun.was.domain.auth.security.AuthenticatedUser;
import kr.withrun.was.domain.user.dto.UpdateUserRunningPreferenceRequest;
import kr.withrun.was.domain.user.dto.UserRunningPreferenceResponse;
import kr.withrun.was.domain.user.service.UserRunningPreferenceService;
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

@RestController
@RequestMapping("/api/users/me/running-preferences")
@RequiredArgsConstructor
public class UserRunningPreferenceController {

    private final UserRunningPreferenceService userRunningPreferenceService;

    @GetMapping
    public ResponseEntity<ApiResponse<UserRunningPreferenceResponse>> getRunningPreference(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        return ApiResponse.successEntity(
                userRunningPreferenceService.getRunningPreference(resolveUserId(authenticatedUser))
        );
    }

    @PutMapping
    public ResponseEntity<ApiResponse<Void>> updateRunningPreference(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpdateUserRunningPreferenceRequest request
    ) {
        userRunningPreferenceService.updateRunningPreference(resolveUserId(authenticatedUser), request);
        return ApiResponse.successEntity(ResponseCode.OK);
    }

    private Long resolveUserId(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.userId() == null) {
            throw new CustomException(ResponseCode.UNAUTHORIZED);
        }

        return authenticatedUser.userId();
    }
}
