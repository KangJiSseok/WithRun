package kr.withrun.was.domain.user.controller;

import kr.withrun.was.domain.user.dto.UserCalendarSummaryResponse;
import kr.withrun.was.domain.user.service.UserCalendarService;
import kr.withrun.was.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/calendars")
@RequiredArgsConstructor
public class UserCalendarController {

    private final UserCalendarService userCalendarService;

    @GetMapping("/{userId}/summary")
    public ResponseEntity<ApiResponse<UserCalendarSummaryResponse>> findSummary(@PathVariable Long userId) {
        return ApiResponse.successEntity(userCalendarService.findSummary(userId));
    }
}
