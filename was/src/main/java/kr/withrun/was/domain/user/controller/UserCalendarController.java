package kr.withrun.was.domain.user.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import kr.withrun.was.domain.user.dto.UserCalendarMonthlyRequest;
import kr.withrun.was.domain.user.dto.UserCalendarMonthlyResponse;
import kr.withrun.was.domain.user.dto.UserCalendarSummaryResponse;
import kr.withrun.was.domain.user.service.UserCalendarService;
import kr.withrun.was.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/calendars")
@RequiredArgsConstructor
public class UserCalendarController {

    private final UserCalendarService userCalendarService;

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserCalendarMonthlyResponse>> findMonthlyCalendar(
            @PathVariable @Positive Long userId,
            @Valid @ModelAttribute UserCalendarMonthlyRequest request
    ) {
        return ApiResponse.successEntity(userCalendarService.findMonthlyCalendar(userId, request.year(), request.month()));
    }

    @GetMapping("/{userId}/summary")
    public ResponseEntity<ApiResponse<UserCalendarSummaryResponse>> findSummary(@PathVariable Long userId) {
        return ApiResponse.successEntity(userCalendarService.findSummary(userId));
    }
}
