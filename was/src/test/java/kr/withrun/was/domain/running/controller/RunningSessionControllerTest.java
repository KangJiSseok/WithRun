package kr.withrun.was.domain.running.controller;

import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.domain.course.vo.GeoPoint;
import kr.withrun.was.domain.running.dto.CreateRunningSessionRequest;
import kr.withrun.was.domain.running.dto.CreateRunningSessionResponse;
import kr.withrun.was.domain.running.dto.GhostRunningResultResponse;
import kr.withrun.was.domain.running.dto.PastRunningSessionItemResponse;
import kr.withrun.was.domain.running.dto.PastRunningSessionsRequest;
import kr.withrun.was.domain.running.dto.PastRunningSessionsResponse;
import kr.withrun.was.domain.running.dto.RegisterRunningSessionCourseRequest;
import kr.withrun.was.domain.running.dto.RegisterRunningSessionCourseResponse;
import kr.withrun.was.domain.running.dto.UpdateRunningSessionRequest;
import kr.withrun.was.domain.running.dto.UpdateRunningSessionResponse;
import kr.withrun.was.domain.running.service.RunningSessionService;
import kr.withrun.was.domain.running.type.GhostResultStatus;
import kr.withrun.was.global.common.type.Difficulty;
import kr.withrun.was.global.common.type.TimeSlot;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
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

import static kr.withrun.was.domain.running.type.RunningMode.COURSE;
import static kr.withrun.was.domain.running.type.RunningMode.GHOST;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    @ValueSource(strings = {"userId", "mode", "startLatitude", "startLongitude"})
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

    @DisplayName("러닝 기록 코스 등록 응답을 생성 성공 포맷으로 감싼다")
    @Test
    void wrapsRegisterRunningSessionCourseResponseInCreatedEnvelope() throws Exception {
        long runningSessionId = 901L;
        RegisterRunningSessionCourseRequest request = validRegisterCourseRequest();
        RegisterRunningSessionCourseResponse response = new RegisterRunningSessionCourseResponse(
                101L,
                "한강 야간 러닝 10K",
                CourseStatus.COMMUNITY,
                Difficulty.MEDIUM,
                10000,
                120,
                "https://cdn.example.com/course/snapshot.png",
                37.566501,
                126.978001,
                37.574501,
                126.989001,
                new Coordinates(java.util.List.of(
                        new GeoPoint(37.566501, 126.978001, 0.0),
                        new GeoPoint(37.574501, 126.989001, 1.0)
                ))
        );
        given(runningSessionService.registerRunningSessionCourse(runningSessionId, request)).willReturn(response);

        mockMvc.perform(post("/api/running-sessions/{runningSessionId}/register-course", runningSessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRegisterCourseRequestBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.CREATED.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.CREATED.getMessage()))
                .andExpect(jsonPath("$.data.courseId").value(101L))
                .andExpect(jsonPath("$.data.status").value(CourseStatus.COMMUNITY.name()))
                .andExpect(jsonPath("$.data.difficulty").value(Difficulty.MEDIUM.name()))
                .andExpect(jsonPath("$.data.coordinates[0].latitude").value(37.566501))
                .andExpect(jsonPath("$.data.coordinates[0].elevationM").value(0.0));

        then(runningSessionService).should().registerRunningSessionCourse(runningSessionId, request);
    }

    @DisplayName("코스 등록 요청에 필수 필드가 없으면 잘못된 입력 응답을 반환한다")
    @ParameterizedTest(name = "누락된 필드: {0}")
    @ValueSource(strings = {"title", "difficulty", "courseTypes"})
    void returnsInvalidInputValueWhenRegisterCourseRequiredFieldIsMissing(String missingField) throws Exception {
        mockMvc.perform(post("/api/running-sessions/{runningSessionId}/register-course", 901L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerCourseRequestBodyWithout(missingField)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INPUT_VALUE.getCode()))
                .andExpect(jsonPath("$.data[0].field").value(missingField));

        then(runningSessionService).shouldHaveNoInteractions();
    }

    @DisplayName("완료되지 않은 세션의 코스 등록 요청은 충돌 응답을 반환한다")
    @Test
    void returnsConflictWhenRegisterCourseRequestedForIncompleteSession() throws Exception {
        long runningSessionId = 901L;
        RegisterRunningSessionCourseRequest request = validRegisterCourseRequest();
        given(runningSessionService.registerRunningSessionCourse(runningSessionId, request))
                .willThrow(new CustomException(ResponseCode.RUNNING_SESSION_NOT_COMPLETED));

        mockMvc.perform(post("/api/running-sessions/{runningSessionId}/register-course", runningSessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRegisterCourseRequestBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.RUNNING_SESSION_NOT_COMPLETED.getCode()));

        then(runningSessionService).should().registerRunningSessionCourse(runningSessionId, request);
    }

    @DisplayName("고스트 러닝 결과 조회 응답을 성공 응답 포맷으로 감싼다")
    @Test
    void wrapsGhostRunningResultResponseInSuccessEnvelope() throws Exception {
        long runningSessionId = 901L;
        GhostRunningResultResponse response = new GhostRunningResultResponse(
                77L,
                runningSessionId,
                345L,
                202L,
                GhostResultStatus.WIN,
                14,
                32,
                LocalDateTime.of(2026, 3, 8, 8, 15)
        );
        given(runningSessionService.findGhostRunningResult(runningSessionId)).willReturn(response);

        mockMvc.perform(get("/api/running-sessions/{runningSessionId}/ghost-result", runningSessionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.OK.getMessage()))
                .andExpect(jsonPath("$.data.ghostRunningResultId").value(77L))
                .andExpect(jsonPath("$.data.runningSessionId").value(runningSessionId))
                .andExpect(jsonPath("$.data.ghostTargetRunningSessionId").value(345L))
                .andExpect(jsonPath("$.data.targetUserId").value(202L))
                .andExpect(jsonPath("$.data.resultStatus").value(GhostResultStatus.WIN.name()))
                .andExpect(jsonPath("$.data.timeGapSec").value(14))
                .andExpect(jsonPath("$.data.distanceGapM").value(32))
                .andExpect(jsonPath("$.data.createdAt").value("2026-03-08T08:15:00"));

        then(runningSessionService).should().findGhostRunningResult(runningSessionId);
    }

    @DisplayName("고스트 러닝 결과가 없으면 찾을 수 없음 응답을 반환한다")
    @Test
    void returnsNotFoundWhenGhostRunningResultDoesNotExist() throws Exception {
        long runningSessionId = 901L;
        given(runningSessionService.findGhostRunningResult(runningSessionId))
                .willThrow(new CustomException(ResponseCode.GHOST_RUNNING_RESULT_NOT_FOUND));

        mockMvc.perform(get("/api/running-sessions/{runningSessionId}/ghost-result", runningSessionId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.GHOST_RUNNING_RESULT_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.GHOST_RUNNING_RESULT_NOT_FOUND.getMessage()));

        then(runningSessionService).should().findGhostRunningResult(runningSessionId);
    }

    @DisplayName("과거 러닝 세션 조회 응답을 성공 응답 포맷으로 감싼다")
    @Test
    void wrapsPastRunningSessionsResponseInSuccessEnvelope() throws Exception {
        PastRunningSessionsRequest request = new PastRunningSessionsRequest(7L, 2026, 3, 16, 10, "cursor-token");
        PastRunningSessionsResponse response = new PastRunningSessionsResponse(
                java.util.List.of(
                        new PastRunningSessionItemResponse(
                                120L,
                                LocalDateTime.of(2026, 3, 15, 19, 30),
                                7200,
                                2100,
                                430,
                                "https://cdn.withrun.app/snapshots/120.png",
                                42,
                                true,
                                GHOST,
                                GhostResultStatus.WIN,
                                TimeSlot.EVENING
                        )
                ),
                true,
                "next-cursor"
        );
        given(runningSessionService.findPastRunningSessions(request)).willReturn(response);

        mockMvc.perform(get("/api/running-sessions/history/past")
                        .queryParam("userId", "7")
                        .queryParam("year", "2026")
                        .queryParam("month", "3")
                        .queryParam("day", "16")
                        .queryParam("pageSize", "10")
                        .queryParam("cursor", "cursor-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.OK.getMessage()))
                .andExpect(jsonPath("$.data.items[0].runningSessionId").value(120L))
                .andExpect(jsonPath("$.data.items[0].runningMode").value(GHOST.name()))
                .andExpect(jsonPath("$.data.items[0].ghostResultStatus").value(GhostResultStatus.WIN.name()))
                .andExpect(jsonPath("$.data.items[0].timeSlot").value(TimeSlot.EVENING.name()))
                .andExpect(jsonPath("$.data.hasMore").value(true))
                .andExpect(jsonPath("$.data.nextCursor").value("next-cursor"));

        then(runningSessionService).should().findPastRunningSessions(request);
    }

    @DisplayName("pageSize가 없으면 기본값 10으로 과거 러닝 세션을 조회한다")
    @Test
    void usesDefaultPageSizeWhenPastRunningSessionsPageSizeIsMissing() throws Exception {
        PastRunningSessionsRequest request = new PastRunningSessionsRequest(7L, null, null, null, 10, null);
        given(runningSessionService.findPastRunningSessions(request))
                .willReturn(new PastRunningSessionsResponse(java.util.List.of(), false, null));

        mockMvc.perform(get("/api/running-sessions/history/past")
                        .queryParam("userId", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.hasMore").value(false));

        then(runningSessionService).should().findPastRunningSessions(request);
    }

    @DisplayName("userId를 포함한 쿼리 기반 과거 조회 요청을 서비스에 전달한다")
    @Test
    void passesQueryUserIdToService() throws Exception {
        PastRunningSessionsRequest request = new PastRunningSessionsRequest(7L, 2026, 3, null, 10, null);
        given(runningSessionService.findPastRunningSessions(request))
                .willReturn(new PastRunningSessionsResponse(java.util.List.of(), false, null));

        mockMvc.perform(get("/api/running-sessions/history/past")
                        .queryParam("userId", "7")
                        .queryParam("year", "2026")
                        .queryParam("month", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        then(runningSessionService).should().findPastRunningSessions(request);
    }

    @DisplayName("month만 전달된 과거 조회는 잘못된 입력 응답을 반환한다")
    @Test
    void returnsBadRequestWhenPastRunningSessionFiltersAreInvalid() throws Exception {
        mockMvc.perform(get("/api/running-sessions/history/past")
                        .queryParam("userId", "7")
                        .queryParam("month", "3"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INPUT_VALUE.getCode()));

        then(runningSessionService).shouldHaveNoInteractions();
    }

    @DisplayName("존재할 수 없는 날짜 조합은 컨트롤러 레벨에서 잘못된 입력 응답을 반환한다")
    @Test
    void returnsBadRequestWhenPastRunningSessionDateIsImpossible() throws Exception {
        mockMvc.perform(get("/api/running-sessions/history/past")
                        .queryParam("userId", "7")
                        .queryParam("year", "2026")
                        .queryParam("month", "2")
                        .queryParam("day", "30"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INPUT_VALUE.getCode()));

        then(runningSessionService).shouldHaveNoInteractions();
    }

    private static CreateRunningSessionRequest validCreateRequest() {
        return new CreateRunningSessionRequest(1L, COURSE, 300L, null, 30.0, 176.0);
    }

    private static CreateRunningSessionRequest ghostCreateRequest() {
        return new CreateRunningSessionRequest(1L, GHOST, 300L, 44L, 30.0, 176.0);
    }

    private static String validCreateRequestBody() {
        return """
                {
                  "userId": 1,
                  "mode": "COURSE",
                  "courseId": 300,
                  "ghostTargetRunningSessionId": null,
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
                      "startLatitude": 30.0,
                      "startLongitude": 176.0
                    }
                    """;
            case "mode" -> """
                    {
                      "userId": 1,
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
                      "startLongitude": 176.0
                    }
                    """;
            case "startLongitude" -> """
                    {
                      "userId": 1,
                      "mode": "COURSE",
                      "courseId": 300,
                      "ghostTargetRunningSessionId": null,
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
                  "startLatitude": %s,
                  "startLongitude": %s
                }
                """.formatted(modeValue, courseIdValue, ghostTargetValue, startLatitudeValue, startLongitudeValue);
    }

    private static RegisterRunningSessionCourseRequest validRegisterCourseRequest() {
        return new RegisterRunningSessionCourseRequest(
                "한강 야간 러닝 10K",
                "MEDIUM",
                java.util.List.of("RIVERSIDE", "URBAN"),
                "https://cdn.example.com/course/snapshot.png"
        );
    }

    private static String validRegisterCourseRequestBody() {
        return """
                {
                  "title": "한강 야간 러닝 10K",
                  "difficulty": "MEDIUM",
                  "courseTypes": ["RIVERSIDE", "URBAN"],
                  "snapshotImageUrl": "https://cdn.example.com/course/snapshot.png"
                }
                """;
    }

    private static String registerCourseRequestBodyWithout(String missingField) {
        return switch (missingField) {
            case "title" -> """
                    {
                      "difficulty": "MEDIUM",
                      "courseTypes": ["RIVERSIDE", "URBAN"],
                      "snapshotImageUrl": "https://cdn.example.com/course/snapshot.png"
                    }
                    """;
            case "difficulty" -> """
                    {
                      "title": "한강 야간 러닝 10K",
                      "courseTypes": ["RIVERSIDE", "URBAN"],
                      "snapshotImageUrl": "https://cdn.example.com/course/snapshot.png"
                    }
                    """;
            case "courseTypes" -> """
                    {
                      "title": "한강 야간 러닝 10K",
                      "difficulty": "MEDIUM",
                      "snapshotImageUrl": "https://cdn.example.com/course/snapshot.png"
                    }
                    """;
            default -> throw new IllegalArgumentException("Unsupported missing field: " + missingField);
        };
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
