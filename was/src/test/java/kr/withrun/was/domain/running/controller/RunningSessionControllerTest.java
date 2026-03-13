package kr.withrun.was.domain.running.controller;

import kr.withrun.was.domain.running.dto.UpdateRunningSessionRequest;
import kr.withrun.was.domain.running.dto.UpdateRunningSessionResponse;
import kr.withrun.was.domain.running.service.RunningSessionService;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RunningSessionController.class, properties = "spring.config.import=")
@DisplayName("러닝 세션 컨트롤러")
class RunningSessionControllerTest {

    static {
        System.setProperty("spring.cloud.aws.parameterstore.enabled", "false");
        System.setProperty("spring.config.import", "");
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RunningSessionService runningSessionService;

    @AfterAll
    static void clearConfigOverrides() {
        System.clearProperty("spring.cloud.aws.parameterstore.enabled");
        System.clearProperty("spring.config.import");
    }

    @DisplayName("러닝 세션 종료 응답을 성공 응답 포맷으로 감싼다")
    @Test
    void wrapsCompleteRunningSessionResponseInSuccessEnvelope() throws Exception {
        long runningSessionId = 901L;
        UpdateRunningSessionResponse response = new UpdateRunningSessionResponse(
                runningSessionId,
                true,
                false,
                LocalDateTime.parse("2026-03-12T06:30:00"),
                LocalDateTime.parse("2026-03-12T07:12:00"),
                10023,
                642,
                30.0,
                150.0,
                11.0,
                2520,
                2,
                37.5,
                127.0,
                3
        );
        given(runningSessionService.completeRunningSession(eq(runningSessionId), any(UpdateRunningSessionRequest.class)))
                .willReturn(response);

        mockMvc.perform(patch("/api/running-sessions/{runningSessionId}", runningSessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.OK.getMessage()))
                .andExpect(jsonPath("$.data.runningSessionId").value(runningSessionId))
                .andExpect(jsonPath("$.data.isCompleted").value(true))
                .andExpect(jsonPath("$.data.isPublic").value(false))
                .andExpect(jsonPath("$.data.startedAt").value("2026-03-12T06:30:00"))
                .andExpect(jsonPath("$.data.endedAt").value("2026-03-12T07:12:00"))
                .andExpect(jsonPath("$.data.distanceM").value(10023))
                .andExpect(jsonPath("$.data.caloriesKcal").value(642))
                .andExpect(jsonPath("$.data.endLatitude").value(30.0))
                .andExpect(jsonPath("$.data.endLongitude").value(150.0))
                .andExpect(jsonPath("$.data.avgSpeedMps").value(11.0))
                .andExpect(jsonPath("$.data.durationSec").value(2520))
                .andExpect(jsonPath("$.data.avgPaceSecPerKm").value(2))
                .andExpect(jsonPath("$.data.startLatitude").value(37.5))
                .andExpect(jsonPath("$.data.startLongitude").value(127.0))
                .andExpect(jsonPath("$.data.elevationGainM").value(3));

        then(runningSessionService).should().completeRunningSession(eq(runningSessionId), any(UpdateRunningSessionRequest.class));
    }

    @DisplayName("이미 종료된 세션은 충돌 응답을 반환한다")
    @Test
    void returnsConflictWhenRunningSessionIsAlreadyCompleted() throws Exception {
        long runningSessionId = 901L;
        given(runningSessionService.completeRunningSession(eq(runningSessionId), any(UpdateRunningSessionRequest.class)))
                .willThrow(new CustomException(ResponseCode.RUNNING_SESSION_ALREADY_COMPLETED));

        mockMvc.perform(patch("/api/running-sessions/{runningSessionId}", runningSessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.RUNNING_SESSION_ALREADY_COMPLETED.getCode()));

        then(runningSessionService).should().completeRunningSession(eq(runningSessionId), any(UpdateRunningSessionRequest.class));
    }

    @DisplayName("존재하지 않는 세션은 찾을 수 없음 응답을 반환한다")
    @Test
    void returnsNotFoundWhenRunningSessionDoesNotExist() throws Exception {
        long runningSessionId = 999L;
        given(runningSessionService.completeRunningSession(eq(runningSessionId), any(UpdateRunningSessionRequest.class)))
                .willThrow(new CustomException(ResponseCode.RUNNING_SESSION_NOT_FOUND));

        mockMvc.perform(patch("/api/running-sessions/{runningSessionId}", runningSessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestJson()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.RUNNING_SESSION_NOT_FOUND.getCode()));

        then(runningSessionService).should().completeRunningSession(eq(runningSessionId), any(UpdateRunningSessionRequest.class));
    }

    @DisplayName("유효하지 않은 종료 요청은 잘못된 입력 응답을 반환한다")
    @Test
    void returnsBadRequestWhenCompletionPayloadIsInvalid() throws Exception {
        long runningSessionId = 901L;

        mockMvc.perform(patch("/api/running-sessions/{runningSessionId}", runningSessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 1,
                                  "endedAt": "2026-03-12T07:12:00",
                                  "distanceM": -1,
                                  "caloriesKcal": 642,
                                  "endLatitude": 30,
                                  "endLongitude": 150,
                                  "avgSpeedMps": 11,
                                  "durationSec": 2520,
                                  "avgPaceSecPerKm": 2,
                                  "elevationGainM": 3
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INPUT_VALUE.getCode()));
    }

    private String validRequestJson() {
        return """
                {
                  "userId": 1,
                  "endedAt": "2026-03-12T07:12:00",
                  "distanceM": 10023,
                  "caloriesKcal": 642,
                  "endLatitude": 30,
                  "endLongitude": 150,
                  "avgSpeedMps": 11,
                  "durationSec": 2520,
                  "avgPaceSecPerKm": 2,
                  "elevationGainM": 3
                }
                """;
    }
}
