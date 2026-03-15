package kr.withrun.was.domain.running.controller;

import kr.withrun.was.domain.running.dto.CreateRunningSessionRequest;
import kr.withrun.was.domain.running.dto.CreateRunningSessionResponse;
import kr.withrun.was.domain.running.dto.AllRunningSessionItemResponse;
import kr.withrun.was.domain.running.dto.AllRunningSessionsRequest;
import kr.withrun.was.domain.running.dto.AllRunningSessionsResponse;
import kr.withrun.was.domain.running.dto.UpdateRunningSessionRequest;
import kr.withrun.was.domain.running.dto.UpdateRunningSessionResponse;
import kr.withrun.was.domain.running.service.RunningSessionService;
import kr.withrun.was.domain.running.type.GhostResultStatus;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import kr.withrun.was.domain.user.type.TimeSlot;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static kr.withrun.was.domain.running.type.RunningMode.COURSE;
import static kr.withrun.was.domain.running.type.RunningMode.GHOST;
import static kr.withrun.was.domain.running.type.RunningMode.FREE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RunningSessionController.class, properties = "spring.config.import=")
@DisplayName("러닝 세션 컨트롤러")
class RunningSessionControllerTest {

    private static final LocalDateTime STARTED_AT = LocalDateTime.of(2026, 3, 12, 6, 30);

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

    @DisplayName("러닝 세션 생성 응답을 생성 성공 포맷으로 감싼다")
    @Test
    void wrapsCreateRunningSessionResponseInCreatedEnvelope() throws Exception {
        CreateRunningSessionRequest request = validCreateRequest();
        CreateRunningSessionResponse response = new CreateRunningSessionResponse(
                901L,
                COURSE,
                300L,
                null,
                0,
                0,
                false
        );
        given(runningSessionService.createRunningSession(request)).willReturn(response);

        mockMvc.perform(post("/api/running-sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequestBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.CREATED.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.CREATED.getMessage()))
                .andExpect(jsonPath("$.data.runningSessionId").value(901L))
                .andExpect(jsonPath("$.data.mode").value(COURSE.name()))
                .andExpect(jsonPath("$.data.courseId").value(300L))
                .andExpect(jsonPath("$.data.ghostTargetRunningSessionId").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.data.startedAt").doesNotExist())
                .andExpect(jsonPath("$.data.distanceM").value(0))
                .andExpect(jsonPath("$.data.caloriesKcal").value(0))
                .andExpect(jsonPath("$.data.isCompleted").value(false));

        then(runningSessionService).should().createRunningSession(request);
    }

    @DisplayName("필수 요청 필드가 없으면 잘못된 입력 응답을 반환한다")
    @ParameterizedTest(name = "누락된 필드: {0}")
    @ValueSource(strings = {"userId", "mode", "startedAt", "startLatitude", "startLongitude"})
    void returnsInvalidInputValueWhenRequiredRequestFieldIsMissing(String missingField) throws Exception {
        mockMvc.perform(post("/api/running-sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyWithout(missingField)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INPUT_VALUE.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.INVALID_INPUT_VALUE.getMessage()))
                .andExpect(jsonPath("$.data[0].field").value(missingField));

        then(runningSessionService).shouldHaveNoInteractions();
    }

    @DisplayName("위도나 경도가 범위를 벗어나면 잘못된 입력 응답을 반환한다")
    @ParameterizedTest(name = "잘못된 {0}: {1}")
    @CsvSource({
            "startLatitude,91.0",
            "startLatitude,-91.0",
            "startLongitude,181.0",
            "startLongitude,-181.0"
    })
    void returnsInvalidInputValueWhenCoordinatesAreOutOfRange(String fieldName, String invalidValue) throws Exception {
        mockMvc.perform(post("/api/running-sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyWith(fieldName, invalidValue)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INPUT_VALUE.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.INVALID_INPUT_VALUE.getMessage()))
                .andExpect(jsonPath("$.data[0].field").value(fieldName));

        then(runningSessionService).shouldHaveNoInteractions();
    }

    @DisplayName("잘못된 러닝 모드가 들어오면 내부 서버 오류 응답을 반환하고 서비스를 호출하지 않는다")
    @Test
    void returnsInternalServerErrorWhenRunningModeIsInvalid() throws Exception {
        mockMvc.perform(post("/api/running-sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBodyWith("mode", "\"MARATHON\"")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INTERNAL_SERVER_ERROR.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.INTERNAL_SERVER_ERROR.getMessage()));

        then(runningSessionService).shouldHaveNoInteractions();
    }

    @DisplayName("서비스가 없는 코스를 감지하면 코스 없음 응답을 반환한다")
    @Test
    void returnsCourseNotFoundWhenServiceRejectsCourse() throws Exception {
        CreateRunningSessionRequest request = validCreateRequest();
        given(runningSessionService.createRunningSession(request))
                .willThrow(new CustomException(ResponseCode.COURSE_NOT_FOUND));

        mockMvc.perform(post("/api/running-sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCreateRequestBody()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.COURSE_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.COURSE_NOT_FOUND.getMessage()));

        then(runningSessionService).should().createRunningSession(request);
    }

    @DisplayName("서비스가 없는 고스트 대상을 감지하면 해당 응답을 반환한다")
    @Test
    void returnsGhostTargetNotFoundWhenServiceRejectsGhostTarget() throws Exception {
        CreateRunningSessionRequest request = ghostCreateRequest();
        given(runningSessionService.createRunningSession(request))
                .willThrow(new CustomException(ResponseCode.GHOST_TARGET_NOT_FOUND));

        mockMvc.perform(post("/api/running-sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ghostCreateRequestBody()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.GHOST_TARGET_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.GHOST_TARGET_NOT_FOUND.getMessage()));

        then(runningSessionService).should().createRunningSession(request);
    }

    @DisplayName("서비스가 유효하지 않은 고스트 대상을 거부하면 해당 응답을 반환한다")
    @Test
    void returnsInvalidGhostTargetWhenServiceRejectsGhostTargetEligibility() throws Exception {
        CreateRunningSessionRequest request = ghostCreateRequest();
        given(runningSessionService.createRunningSession(request))
                .willThrow(new CustomException(ResponseCode.INVALID_GHOST_TARGET));

        mockMvc.perform(post("/api/running-sessions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ghostCreateRequestBody()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_GHOST_TARGET.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.INVALID_GHOST_TARGET.getMessage()));

        then(runningSessionService).should().createRunningSession(request);
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
                        .content(validUpdateRequestJson()))
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
                        .content(validUpdateRequestJson()))
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
                        .content(validUpdateRequestJson()))
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

    @DisplayName("러닝 세션 히스토리 조회 응답을 성공 응답 포맷으로 감싼다")
    @Test
    void wrapsRunningSessionHistoryResponseInSuccessEnvelope() throws Exception {
        long userId = 20L;
        AllRunningSessionsRequest request = new AllRunningSessionsRequest(userId, 2, "previous-cursor");
        AllRunningSessionsResponse response = new AllRunningSessionsResponse(
                List.of(
                        new AllRunningSessionItemResponse(
                                120L,
                                7200,
                                2100,
                                430,
                                "https://cdn.withrun.app/snapshots/120.png",
                                42,
                                true,
                                GHOST,
                                GhostResultStatus.WIN,
                                TimeSlot.EVENING
                        ),
                        new AllRunningSessionItemResponse(
                                119L,
                                5000,
                                null,
                                300,
                                null,
                                18,
                                false,
                                FREE,
                                null,
                                TimeSlot.MORNING
                        )
                ),
                true,
                "next-cursor"
        );
        given(runningSessionService.findAllRunningSessions(request)).willReturn(response);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/running-sessions/history/{userId}/all", userId)
                        .queryParam("pageSize", "2")
                        .queryParam("cursor", "previous-cursor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.OK.getMessage()))
                .andExpect(jsonPath("$.data.items[0].runningSessionId").value(120L))
                .andExpect(jsonPath("$.data.items[0].distanceM").value(7200))
                .andExpect(jsonPath("$.data.items[0].durationSec").value(2100))
                .andExpect(jsonPath("$.data.items[0].caloriesKcal").value(430))
                .andExpect(jsonPath("$.data.items[0].snapshotImageUrl").value("https://cdn.withrun.app/snapshots/120.png"))
                .andExpect(jsonPath("$.data.items[0].elevationGainM").value(42))
                .andExpect(jsonPath("$.data.items[0].isCompleted").value(true))
                .andExpect(jsonPath("$.data.items[0].runningMode").value(GHOST.name()))
                .andExpect(jsonPath("$.data.items[0].ghostResultStatus").value(GhostResultStatus.WIN.name()))
                .andExpect(jsonPath("$.data.items[0].timeSlot").value(TimeSlot.EVENING.name()))
                .andExpect(jsonPath("$.data.items[1].runningMode").value(FREE.name()))
                .andExpect(jsonPath("$.data.items[1].ghostResultStatus").value(Matchers.nullValue()))
                .andExpect(jsonPath("$.data.hasMore").value(true))
                .andExpect(jsonPath("$.data.nextCursor").value("next-cursor"));

        then(runningSessionService).should().findAllRunningSessions(request);
    }

    @DisplayName("pageSize를 생략하면 기본값 10으로 러닝 세션 히스토리를 조회한다")
    @Test
    void usesDefaultPageSizeWhenRunningSessionHistoryPageSizeIsOmitted() throws Exception {
        AllRunningSessionsRequest request = new AllRunningSessionsRequest(20L, 10, null);
        AllRunningSessionsResponse response = new AllRunningSessionsResponse(List.of(), false, null);
        given(runningSessionService.findAllRunningSessions(request)).willReturn(response);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/running-sessions/history/{userId}/all", 20L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()));

        then(runningSessionService).should().findAllRunningSessions(request);
    }

    @DisplayName("pageSize가 빈 문자열이면 기본값 10으로 러닝 세션 히스토리를 조회한다")
    @Test
    void usesDefaultPageSizeWhenRunningSessionHistoryPageSizeIsBlank() throws Exception {
        AllRunningSessionsRequest request = new AllRunningSessionsRequest(20L, 10, null);
        AllRunningSessionsResponse response = new AllRunningSessionsResponse(List.of(), false, null);
        given(runningSessionService.findAllRunningSessions(request)).willReturn(response);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/running-sessions/history/{userId}/all", 20L)
                        .queryParam("pageSize", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()));

        then(runningSessionService).should().findAllRunningSessions(request);
    }

    @DisplayName("pageSize가 유효하지 않으면 잘못된 입력 응답을 반환한다")
    @ParameterizedTest(name = "pageSize={0}")
    @ValueSource(strings = {"0", "-1", "51", "abc", "1.5"})
    void returnsInvalidInputWhenRunningSessionHistoryPageSizeIsInvalid(String invalidPageSize) throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/running-sessions/history/{userId}/all", 20L)
                        .queryParam("pageSize", invalidPageSize))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INPUT_VALUE.getCode()));

        then(runningSessionService).shouldHaveNoInteractions();
    }

    @DisplayName("userId가 유효하지 않으면 잘못된 입력 응답을 반환한다")
    @Test
    void returnsInvalidInputWhenRunningSessionHistoryUserIdIsInvalid() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/running-sessions/history/{userId}/all", 0L))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INPUT_VALUE.getCode()));

        then(runningSessionService).shouldHaveNoInteractions();
    }

    @DisplayName("서비스가 커서를 거부하면 잘못된 커서 응답을 반환한다")
    @Test
    void returnsInvalidCursorWhenRunningSessionHistoryServiceRejectsCursor() throws Exception {
        AllRunningSessionsRequest request = new AllRunningSessionsRequest(20L, 10, "%%%");
        given(runningSessionService.findAllRunningSessions(request))
                .willThrow(new CustomException(ResponseCode.INVALID_CURSOR));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/running-sessions/history/{userId}/all", 20L)
                        .queryParam("cursor", "%%%"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_CURSOR.getCode()));

        then(runningSessionService).should().findAllRunningSessions(request);
    }

    private static CreateRunningSessionRequest validCreateRequest() {
        return new CreateRunningSessionRequest(1L, COURSE, 300L, null, STARTED_AT, 30.0, 176.0);
    }

    private static CreateRunningSessionRequest ghostCreateRequest() {
        return new CreateRunningSessionRequest(1L, GHOST, 300L, 44L, STARTED_AT, 30.0, 176.0);
    }

    private static String validCreateRequestBody() {
        return """
                {
                  "userId": 1,
                  "mode": "COURSE",
                  "courseId": 300,
                  "ghostTargetRunningSessionId": null,
                  "startedAt": "2026-03-12T06:30:00",
                  "startLatitude": 30.0,
                  "startLongitude": 176.0
                }
                """;
    }

    private static String ghostCreateRequestBody() {
        return """
                {
                  "userId": 1,
                  "mode": "GHOST",
                  "courseId": 300,
                  "ghostTargetRunningSessionId": 44,
                  "startedAt": "2026-03-12T06:30:00",
                  "startLatitude": 30.0,
                  "startLongitude": 176.0
                }
                """;
    }

    private static String requestBodyWithout(String missingField) {
        return switch (missingField) {
            case "userId" -> """
                    {
                      "mode": "COURSE",
                      "courseId": 300,
                      "ghostTargetRunningSessionId": null,
                      "startedAt": "2026-03-12T06:30:00",
                      "startLatitude": 30.0,
                      "startLongitude": 176.0
                    }
                    """;
            case "mode" -> """
                    {
                      "userId": 1,
                      "startedAt": "2026-03-12T06:30:00",
                      "courseId": 300,
                      "ghostTargetRunningSessionId": null,
                      "startLatitude": 30.0,
                      "startLongitude": 176.0
                    }
                    """;
            case "startedAt" -> """
                    {
                      "userId": 1,
                      "mode": "COURSE",
                      "courseId": 300,
                      "ghostTargetRunningSessionId": null,
                      "startLatitude": 30.0,
                      "startLongitude": 176.0
                    }
                    """;
            case "startLatitude" -> """
                    {
                      "userId": 1,
                      "mode": "COURSE",
                      "courseId": 300,
                      "ghostTargetRunningSessionId": null,
                      "startedAt": "2026-03-12T06:30:00",
                      "startLongitude": 176.0
                    }
                    """;
            case "startLongitude" -> """
                    {
                      "userId": 1,
                      "mode": "COURSE",
                      "courseId": 300,
                      "ghostTargetRunningSessionId": null,
                      "startedAt": "2026-03-12T06:30:00",
                      "startLatitude": 30.0
                    }
                    """;
            default -> throw new IllegalArgumentException("Unsupported missing field: " + missingField);
        };
    }

    private static String requestBodyWith(String fieldName, String value) {
        String modeValue = "\"COURSE\"";
        String courseIdValue = "300";
        String ghostTargetValue = "null";
        String startedAtValue = "\"2026-03-12T06:30:00\"";
        String startLatitudeValue = "30.0";
        String startLongitudeValue = "176.0";

        if ("mode".equals(fieldName)) {
            modeValue = value;
        }
        if ("courseId".equals(fieldName)) {
            courseIdValue = value;
        }
        if ("ghostTargetRunningSessionId".equals(fieldName)) {
            ghostTargetValue = value;
        }
        if ("startedAt".equals(fieldName)) {
            startedAtValue = value;
        }
        if ("startLatitude".equals(fieldName)) {
            startLatitudeValue = value;
        }
        if ("startLongitude".equals(fieldName)) {
            startLongitudeValue = value;
        }

        return """
                {
                  "userId": 1,
                  "mode": %s,
                  "courseId": %s,
                  "ghostTargetRunningSessionId": %s,
                  "startedAt": %s,
                  "startLatitude": %s,
                  "startLongitude": %s
                }
                """.formatted(modeValue, courseIdValue, ghostTargetValue, startedAtValue, startLatitudeValue, startLongitudeValue);
    }

    private static String validUpdateRequestJson() {
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
