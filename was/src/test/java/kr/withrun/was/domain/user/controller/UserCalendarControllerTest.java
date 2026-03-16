package kr.withrun.was.domain.user.controller;

import kr.withrun.was.domain.user.dto.UserCalendarSummaryResponse;
import kr.withrun.was.domain.user.service.UserCalendarService;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = UserCalendarController.class, properties = "spring.config.import=")
@DisplayName("사용자 캘린더 컨트롤러")
class UserCalendarControllerTest {

    static {
        System.setProperty("spring.cloud.aws.parameterstore.enabled", "false");
        System.setProperty("spring.config.import", "");
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserCalendarService userCalendarService;

    @AfterAll
    static void clearConfigOverrides() {
        System.clearProperty("spring.cloud.aws.parameterstore.enabled");
        System.clearProperty("spring.config.import");
    }

    @DisplayName("캘린더 요약 응답을 성공 응답 포맷으로 감싼다")
    @Test
    void wrapsCalendarSummaryResponseInSuccessEnvelope() throws Exception {
        long userId = 30L;
        UserCalendarSummaryResponse response = new UserCalendarSummaryResponse(
                128,
                742300,
                58,
                47,
                23,
                6,
                21,
                LocalDate.of(2026, 3, 15)
        );
        given(userCalendarService.findSummary(userId)).willReturn(response);

        mockMvc.perform(get("/api/calendars/{userId}/summary", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.OK.getMessage()))
                .andExpect(jsonPath("$.data.lifetimeRunCount").value(128))
                .andExpect(jsonPath("$.data.lifetimeDistanceM").value(742300))
                .andExpect(jsonPath("$.data.lifetimeCourseRunCount").value(58))
                .andExpect(jsonPath("$.data.lifetimeFreeRunCount").value(47))
                .andExpect(jsonPath("$.data.lifetimeGhostRunCount").value(23))
                .andExpect(jsonPath("$.data.currentStreakDays").value(6))
                .andExpect(jsonPath("$.data.longestStreakDays").value(21))
                .andExpect(jsonPath("$.data.calculatedDate").value("2026-03-15"));

        then(userCalendarService).should().findSummary(userId);
    }

    @DisplayName("없는 사용자를 조회하면 사용자 없음 응답을 반환한다")
    @Test
    void returnsUserNotFoundWhenUserIsMissing() throws Exception {
        long userId = 999L;
        given(userCalendarService.findSummary(userId))
                .willThrow(new CustomException(ResponseCode.USER_NOT_FOUND));

        mockMvc.perform(get("/api/calendars/{userId}/summary", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.USER_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.USER_NOT_FOUND.getMessage()));

        then(userCalendarService).should().findSummary(userId);
    }
}
