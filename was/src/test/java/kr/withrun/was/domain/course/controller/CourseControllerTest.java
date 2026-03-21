package kr.withrun.was.domain.course.controller;

import kr.withrun.was.domain.course.dto.CourseDetailResponse;
import kr.withrun.was.domain.course.dto.CourseFilterResponse;
import kr.withrun.was.domain.course.dto.CourseGhostDetailResponse;
import kr.withrun.was.domain.course.dto.CourseGhostLeaderboardItemResponse;
import kr.withrun.was.domain.course.dto.CourseGhostLeaderboardRequest;
import kr.withrun.was.domain.course.dto.CourseGhostLeaderboardResponse;
import kr.withrun.was.domain.course.dto.CourseLeaderboardItemResponse;
import kr.withrun.was.domain.course.dto.CourseLeaderboardRequest;
import kr.withrun.was.domain.course.dto.CourseLeaderboardResponse;
import kr.withrun.was.domain.course.dto.CourseSurveyResponse;
import kr.withrun.was.domain.course.dto.NearbyCourseItemResponse;
import kr.withrun.was.domain.course.dto.NearbyCoursesRequest;
import kr.withrun.was.domain.course.dto.NearbyCoursesResponse;
import kr.withrun.was.domain.course.dto.NearbyGhostCoursesRequest;
import kr.withrun.was.domain.course.dto.NearbyGhostCoursesResponse;
import kr.withrun.was.domain.course.dto.PreferredDistanceRange;
import kr.withrun.was.domain.course.service.CourseService;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.domain.course.vo.GeoPoint;
import kr.withrun.was.global.common.type.Difficulty;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CourseController.class, properties = "spring.config.import=")
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("코스 조회 컨트롤러")
class CourseControllerTest {

    static {
        System.setProperty("spring.cloud.aws.parameterstore.enabled", "false");
        System.setProperty("spring.config.import", "");
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseService courseService;

    @AfterAll
    static void clearConfigOverrides() {
        System.clearProperty("spring.cloud.aws.parameterstore.enabled");
        System.clearProperty("spring.config.import");
    }

    @DisplayName("필수 쿼리 파라미터가 없으면 잘못된 입력 응답을 반환한다")
    @ParameterizedTest(name = "누락된 파라미터: {0}")
    @ValueSource(strings = {"latitude", "longitude", "radiusM", "targetDistanceM"})
    void returnsInvalidInputValueWhenRequiredQueryParameterIsMissing(String missingParameter) throws Exception {
        mockMvc.perform(getNearbyCoursesWithout(missingParameter))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INPUT_VALUE.getCode()))
                .andExpect(jsonPath("$.data[0].field").value(missingParameter));

        then(courseService).should(never()).findNearbyCourses(any());
    }

    @DisplayName("위도나 경도가 범위를 벗어나면 잘못된 입력 응답을 반환한다")
    @ParameterizedTest(name = "잘못된 {0}: {1}")
    @CsvSource({
            "latitude,91.0",
            "latitude,-91.0",
            "longitude,181.0",
            "longitude,-181.0"
    })
    void returnsInvalidInputValueWhenLatitudeOrLongitudeIsOutOfRange(String parameterName, String invalidValue) throws Exception {
        mockMvc.perform(getNearbyCoursesWith(parameterName, invalidValue))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INPUT_VALUE.getCode()))
                .andExpect(jsonPath("$.data[0].field").value(parameterName));

        then(courseService).should(never()).findNearbyCourses(any());
    }

    @DisplayName("반경이 허용 범위를 벗어나면 잘못된 반경 응답을 반환한다")
    @ParameterizedTest(name = "radiusM={0}")
    @ValueSource(strings = {"999", "6000"})
    void returnsInvalidRadiusWhenRadiusIsOutOfRange(String invalidRadius) throws Exception {
        mockMvc.perform(nearbyCoursesRequest("37.5665", "126.9780", invalidRadius, "10000", null, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_RADIUS.getCode()));

        then(courseService).should(never()).findNearbyCourses(any());
    }

    @DisplayName("반경이 정수가 아니면 잘못된 반경 응답을 반환한다")
    @ParameterizedTest(name = "radiusM={0}")
    @ValueSource(strings = {"abc", "1.5"})
    void returnsInvalidRadiusWhenRadiusIsNotAnInteger(String invalidRadius) throws Exception {
        mockMvc.perform(nearbyCoursesRequest("37.5665", "126.9780", invalidRadius, "10000", null, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_RADIUS.getCode()));

        then(courseService).should(never()).findNearbyCourses(any());
    }

    @DisplayName("목표 거리가 유효하지 않으면 잘못된 목표 거리 응답을 반환한다")
    @ParameterizedTest(name = "targetDistanceM={0}")
    @ValueSource(strings = {"0", "-1"})
    void returnsInvalidTargetDistanceWhenTargetDistanceIsInvalid(String invalidTargetDistance) throws Exception {
        mockMvc.perform(nearbyCoursesRequest("37.5665", "126.9780", "1000", invalidTargetDistance, null, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_TARGET_DISTANCE.getCode()));

        then(courseService).should(never()).findNearbyCourses(any());
    }

    @DisplayName("목표 거리가 정수가 아니면 잘못된 목표 거리 응답을 반환한다")
    @ParameterizedTest(name = "targetDistanceM={0}")
    @ValueSource(strings = {"abc", "1.5"})
    void returnsInvalidTargetDistanceWhenTargetDistanceIsNotAnInteger(String invalidTargetDistance) throws Exception {
        mockMvc.perform(nearbyCoursesRequest("37.5665", "126.9780", "1000", invalidTargetDistance, null, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_TARGET_DISTANCE.getCode()));

        then(courseService).should(never()).findNearbyCourses(any());
    }

    @DisplayName("페이지 크기가 유효하지 않으면 잘못된 입력 응답을 반환한다")
    @ParameterizedTest(name = "pageSize={0}")
    @ValueSource(strings = {"0", "-1", "11", "abc", "1.5"})
    void returnsInvalidInputValueWhenPageSizeIsInvalid(String invalidPageSize) throws Exception {
        mockMvc.perform(nearbyCoursesRequest("37.5665", "126.9780", "1000", "10000", invalidPageSize, null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INPUT_VALUE.getCode()));

        then(courseService).should(never()).findNearbyCourses(any());
    }

    @DisplayName("서비스가 커서를 거부하면 잘못된 커서 응답을 반환한다")
    @ParameterizedTest(name = "cursor={0}")
    @ValueSource(strings = {"%%%", "invalid-cursor"})
    void returnsInvalidCursorWhenServiceRejectsCursor(String invalidCursor) throws Exception {
        NearbyCoursesRequest request = new NearbyCoursesRequest(37.5665, 126.9780, 1000, 10000, null, invalidCursor);
        given(courseService.findNearbyCourses(request))
                .willThrow(new CustomException(ResponseCode.INVALID_CURSOR));

        mockMvc.perform(nearbyCoursesRequest("37.5665", "126.9780", "1000", "10000", null, invalidCursor))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_CURSOR.getCode()));

        then(courseService).should().findNearbyCourses(request);
    }

    @DisplayName("주변 코스 응답을 성공 응답 포맷으로 감싼다")
    @ParameterizedTest(name = "radiusM={0}")
    @ValueSource(strings = {"1000", "1500", "5000"})
    void wrapsNearbyCoursesResponseInSuccessEnvelope(String radiusM) throws Exception {
        NearbyCoursesRequest request = new NearbyCoursesRequest(37.5665, 126.9780, Integer.parseInt(radiusM), 10000, null, "bmV4dA");
        NearbyCoursesResponse response = new NearbyCoursesResponse(
                List.of(new NearbyCourseItemResponse(
                        1L,
                        "Han River",
                        CourseStatus.OFFICIAL,
                        10000,
                        35,
                        37.57,
                        126.97,
                        37.58,
                        126.98,
                        240,
                        new NearbyCourseItemResponse.DifficultyOption("MEDIUM", "보통"),
                        List.of(
                                new NearbyCourseItemResponse.CourseTypeOption("RIVERSIDE", "강변"),
                                new NearbyCourseItemResponse.CourseTypeOption("PARK", "공원")
                        ),
                        "https://example.com/course.jpg"
                )),
                true,
                "next-cursor",
                2000
        );
        given(courseService.findNearbyCourses(request)).willReturn(response);

        mockMvc.perform(nearbyCoursesRequest("37.5665", "126.9780", radiusM, "10000", null, "bmV4dA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.OK.getMessage()))
                .andExpect(jsonPath("$.data.items[0].courseId").value(1L))
                .andExpect(jsonPath("$.data.items[0].title").value("Han River"))
                .andExpect(jsonPath("$.data.items[0].status").value(CourseStatus.OFFICIAL.name()))
                .andExpect(jsonPath("$.data.items[0].difficulty.data").value("MEDIUM"))
                .andExpect(jsonPath("$.data.items[0].difficulty.label").value("보통"))
                .andExpect(jsonPath("$.data.items[0].courseTypes[0].data").value("RIVERSIDE"))
                .andExpect(jsonPath("$.data.items[0].courseTypes[0].label").value("강변"))
                .andExpect(jsonPath("$.data.hasMore").value(true))
                .andExpect(jsonPath("$.data.nextCursor").value("next-cursor"))
                .andExpect(jsonPath("$.data.suggestedNextRadiusM").value(2000));

        then(courseService).should().findNearbyCourses(request);
    }

    @DisplayName("코스 설문 응답을 성공 응답 포맷으로 감싼다")
    @Test
    void wrapsCourseSurveysResponseInSuccessEnvelope() throws Exception {
        CourseSurveyResponse response = new CourseSurveyResponse(
                List.of(
                        new CourseSurveyResponse.CourseTypeOption("RIVERSIDE", "강변"),
                        new CourseSurveyResponse.CourseTypeOption("PARK", "공원")
                ),
                List.of(
                        new CourseSurveyResponse.DifficultyOption("EASY", "쉬움"),
                        new CourseSurveyResponse.DifficultyOption("MEDIUM", "보통")
                )
        );
        given(courseService.findCourseSurveys()).willReturn(response);

        mockMvc.perform(get("/api/courses/surveys"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.OK.getMessage()))
                .andExpect(jsonPath("$.data.courseTypes[0].data").value("RIVERSIDE"))
                .andExpect(jsonPath("$.data.courseTypes[0].label").value("강변"))
                .andExpect(jsonPath("$.data.difficulties[0].data").value("EASY"))
                .andExpect(jsonPath("$.data.difficulties[0].label").value("쉬움"));

        then(courseService).should().findCourseSurveys();
    }

    @DisplayName("코스 필터 응답을 성공 응답 포맷으로 감싼다")
    @Test
    void wrapsCourseFiltersResponseInSuccessEnvelope() throws Exception {
        CourseFilterResponse response = new CourseFilterResponse(
                List.of(
                        new CourseFilterResponse.CourseDistanceTypeOption("1", "1km"),
                        new CourseFilterResponse.CourseDistanceTypeOption("3", "3km")
                ),
                List.of(
                        new CourseFilterResponse.CourseTypeOption("RIVERSIDE", "강변"),
                        new CourseFilterResponse.CourseTypeOption("PARK", "공원")
                ),
                List.of(
                        new CourseFilterResponse.DifficultyOption("EASY", "쉬움"),
                        new CourseFilterResponse.DifficultyOption("MEDIUM", "보통")
                )
        );
        given(courseService.findCourseFilters()).willReturn(response);

        mockMvc.perform(get("/api/courses/filter/normal-run"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.OK.getMessage()))
                .andExpect(jsonPath("$.data.courseDistanceTypes[0].data").value("1"))
                .andExpect(jsonPath("$.data.courseDistanceTypes[0].label").value("1km"))
                .andExpect(jsonPath("$.data.courseTypes[0].data").value("RIVERSIDE"))
                .andExpect(jsonPath("$.data.courseTypes[0].label").value("강변"))
                .andExpect(jsonPath("$.data.difficulties[0].data").value("EASY"))
                .andExpect(jsonPath("$.data.difficulties[0].label").value("쉬움"));

        then(courseService).should().findCourseFilters();
    }

    @DisplayName("코스 상세 조회 응답을 성공 응답 포맷으로 감싼다")
    @ParameterizedTest(name = "courseId={0}")
    @ValueSource(longs = {1L, 25L})
    void wrapsCourseDetailResponseInSuccessEnvelope(long courseId) throws Exception {
        CourseDetailResponse response = new CourseDetailResponse(
                courseId,
                "Han River",
                CourseStatus.OFFICIAL,
                new CourseDetailResponse.DifficultyOption("MEDIUM", "보통"),
                10000,
                35,
                "https://example.com/course.jpg",
                37.57,
                126.97,
                37.58,
                126.98,
                new Coordinates(List.of(
                        new GeoPoint(37.57, 126.97, 10.0),
                        new GeoPoint(37.58, 126.98, 12.0)
                )),
                List.of(
                        new CourseDetailResponse.CourseTypeOption("RIVERSIDE", "강변"),
                        new CourseDetailResponse.CourseTypeOption("PARK", "공원")
                ),
                14L,
                false,
                false,
                4.5
        );
        given(courseService.findCourseDetail(courseId)).willReturn(response);

        mockMvc.perform(get("/api/courses/{courseId}", courseId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                .andExpect(jsonPath("$.data.courseId").value(courseId))
                .andExpect(jsonPath("$.data.title").value("Han River"))
                .andExpect(jsonPath("$.data.difficulty.data").value("MEDIUM"))
                .andExpect(jsonPath("$.data.difficulty.label").value("보통"))
                .andExpect(jsonPath("$.data.coordinates[0].latitude").value(37.57))
                .andExpect(jsonPath("$.data.coordinates[1].longitude").value(126.98))
                .andExpect(jsonPath("$.data.coordinates[0].elevationM").value(10.0))
                .andExpect(jsonPath("$.data.courseTypes[0].data").value("RIVERSIDE"))
                .andExpect(jsonPath("$.data.courseTypes[0].label").value("강변"))
                .andExpect(jsonPath("$.data.likeCount").value(14))
                .andExpect(jsonPath("$.data.isLiked").value(false))
                .andExpect(jsonPath("$.data.isBookmarked").value(false))
                .andExpect(jsonPath("$.data.averageRating").value(4.5));

        then(courseService).should().findCourseDetail(courseId);
    }

    @DisplayName("없는 코스 상세 조회는 코스 없음 응답을 반환한다")
    @ParameterizedTest(name = "courseId={0}")
    @ValueSource(longs = {999L})
    void returnsCourseNotFoundWhenCourseDetailIsMissing(long courseId) throws Exception {
        given(courseService.findCourseDetail(courseId))
                .willThrow(new CustomException(ResponseCode.COURSE_NOT_FOUND));

        mockMvc.perform(get("/api/courses/{courseId}", courseId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.COURSE_NOT_FOUND.getCode()));

        then(courseService).should().findCourseDetail(courseId);
    }

    @DisplayName("코스 고스트 상세 조회 응답을 성공 응답 포맷으로 감싼다")
    @Test
    void wrapsCourseGhostDetailResponseInSuccessEnvelope() throws Exception {
        long courseId = 12L;
        long userId = 7L;
        CourseGhostDetailResponse response = new CourseGhostDetailResponse(
                courseId,
                "Han River Ghost",
                CourseStatus.OFFICIAL,
                new CourseGhostDetailResponse.DifficultyOption("MEDIUM", "보통"),
                5200,
                48,
                "https://example.com/ghost-course.jpg",
                37.5661,
                126.9738,
                37.5702,
                126.9814,
                new Coordinates(List.of(
                        new GeoPoint(37.5661, 126.9738, 12.3),
                        new GeoPoint(37.5702, 126.9814, 15.8)
                )),
                List.of(new CourseGhostDetailResponse.CourseTypeOption("RIVERSIDE", "강변")),
                128L,
                false,
                false,
                4.3,
                new CourseGhostDetailResponse.MyRecord(
                        205L,
                        2002L,
                        1320,
                        980,
                        14L,
                        LocalDateTime.of(2026, 3, 20, 7, 10)
                )
        );
        given(courseService.findCourseGhostDetail(courseId, userId)).willReturn(response);

        mockMvc.perform(get("/api/courses/{courseId}/ghost-detail/{userId}", courseId, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                .andExpect(jsonPath("$.data.courseId").value(courseId))
                .andExpect(jsonPath("$.data.title").value("Han River Ghost"))
                .andExpect(jsonPath("$.data.difficulty.data").value("MEDIUM"))
                .andExpect(jsonPath("$.data.courseTypes[0].data").value("RIVERSIDE"))
                .andExpect(jsonPath("$.data.myRecord.leaderboardId").value(205L))
                .andExpect(jsonPath("$.data.myRecord.runningSessionId").value(2002L))
                .andExpect(jsonPath("$.data.myRecord.point").value(980))
                .andExpect(jsonPath("$.data.myRecord.rank").value(14));

        then(courseService).should().findCourseGhostDetail(courseId, userId);
    }

    @DisplayName("없는 코스 고스트 상세 조회는 코스 없음 응답을 반환한다")
    @Test
    void returnsCourseNotFoundWhenCourseGhostDetailIsMissing() throws Exception {
        long courseId = 999L;
        long userId = 7L;
        given(courseService.findCourseGhostDetail(courseId, userId))
                .willThrow(new CustomException(ResponseCode.COURSE_NOT_FOUND));

        mockMvc.perform(get("/api/courses/{courseId}/ghost-detail/{userId}", courseId, userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.COURSE_NOT_FOUND.getCode()));

        then(courseService).should().findCourseGhostDetail(courseId, userId);
    }

    @DisplayName("페이지 크기 쿼리 파라미터를 요청 DTO에 바인딩한다")
    @ParameterizedTest(name = "pageSize={0}")
    @ValueSource(strings = {"2", "10"})
    void bindsPageSizeQueryParameterToRequestDto(String pageSize) throws Exception {
        NearbyCoursesRequest request = new NearbyCoursesRequest(37.5665, 126.9780, 1000, 10000, Integer.parseInt(pageSize), null);
        NearbyCoursesResponse response = new NearbyCoursesResponse(List.of(), false, null, 2000);
        given(courseService.findNearbyCourses(request)).willReturn(response);

        mockMvc.perform(nearbyCoursesRequest("37.5665", "126.9780", "1000", "10000", pageSize, null))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        then(courseService).should().findNearbyCourses(request);
    }

    @DisplayName("코스 리더보드 조회 응답을 성공 응답 포맷으로 감싼다")
    @Test
    void wrapsCourseLeaderboardResponseInSuccessEnvelope() throws Exception {
        CourseLeaderboardRequest request = new CourseLeaderboardRequest(12L, 0, 2);
        CourseLeaderboardResponse response = new CourseLeaderboardResponse(
                12L,
                List.of(new CourseLeaderboardItemResponse(
                        7L,
                        3L,
                        "runner-a",
                        501L,
                        1200,
                        1L,
                        LocalDateTime.of(2026, 3, 18, 9, 0)
                )),
                0,
                2,
                5L,
                3,
                true
        );
        given(courseService.findCourseLeaderboard(request)).willReturn(response);

        mockMvc.perform(get("/api/courses/leaderboards")
                        .queryParam("courseId", "12")
                        .queryParam("page", "0")
                        .queryParam("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                .andExpect(jsonPath("$.data.courseId").value(12L))
                .andExpect(jsonPath("$.data.items[0].leaderboardId").value(7L))
                .andExpect(jsonPath("$.data.items[0].userId").value(3L))
                .andExpect(jsonPath("$.data.items[0].nickname").value("runner-a"))
                .andExpect(jsonPath("$.data.items[0].point").value(1200))
                .andExpect(jsonPath("$.data.items[0].rank").value(1))
                .andExpect(jsonPath("$.data.hasNext").value(true));

        then(courseService).should().findCourseLeaderboard(request);
    }

    @DisplayName("코스 고스트 리더보드 조회 응답을 성공 응답 포맷으로 감싼다")
    @Test
    void wrapsCourseGhostLeaderboardResponseInSuccessEnvelope() throws Exception {
        CourseGhostLeaderboardRequest request = new CourseGhostLeaderboardRequest(20, "b3BhcXVlLWN1cnNvcg");
        CourseGhostLeaderboardResponse response = new CourseGhostLeaderboardResponse(
                List.of(new CourseGhostLeaderboardItemResponse(
                        101L,
                        7L,
                        "runner-a",
                        1001L,
                        1250,
                        1L,
                        LocalDateTime.of(2026, 3, 21, 10, 30)
                )),
                true,
                "opaque-cursor"
        );
        given(courseService.findCourseGhostLeaderboard(12L, request)).willReturn(response);

        mockMvc.perform(get("/api/courses/{courseId}/ghost-leaderboard", 12L)
                        .queryParam("size", "20")
                        .queryParam("cursor", "b3BhcXVlLWN1cnNvcg"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                .andExpect(jsonPath("$.data.items[0].leaderboardId").value(101L))
                .andExpect(jsonPath("$.data.items[0].userId").value(7L))
                .andExpect(jsonPath("$.data.items[0].nickname").value("runner-a"))
                .andExpect(jsonPath("$.data.items[0].point").value(1250))
                .andExpect(jsonPath("$.data.items[0].rank").value(1))
                .andExpect(jsonPath("$.data.hasMore").value(true))
                .andExpect(jsonPath("$.data.nextCursor").value("opaque-cursor"));

        then(courseService).should().findCourseGhostLeaderboard(12L, request);
    }

    @DisplayName("코스 고스트 리더보드 size가 유효하지 않으면 잘못된 입력 응답을 반환한다")
    @ParameterizedTest(name = "size={0}")
    @ValueSource(strings = {"0", "-1", "51", "abc", "1.5"})
    void returnsInvalidInputWhenCourseGhostLeaderboardSizeIsInvalid(String invalidSize) throws Exception {
        mockMvc.perform(get("/api/courses/{courseId}/ghost-leaderboard", 12L)
                        .queryParam("size", invalidSize))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INPUT_VALUE.getCode()));

        then(courseService).should(never()).findCourseGhostLeaderboard(any(), any());
    }

    @DisplayName("코스 고스트 리더보드 조회에서 서비스가 커서를 거부하면 잘못된 커서 응답을 반환한다")
    @Test
    void returnsInvalidCursorWhenCourseGhostLeaderboardServiceRejectsCursor() throws Exception {
        CourseGhostLeaderboardRequest request = new CourseGhostLeaderboardRequest(20, "%%%");
        given(courseService.findCourseGhostLeaderboard(12L, request))
                .willThrow(new CustomException(ResponseCode.INVALID_CURSOR));

        mockMvc.perform(get("/api/courses/{courseId}/ghost-leaderboard", 12L)
                        .queryParam("cursor", "%%%"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_CURSOR.getCode()));

        then(courseService).should().findCourseGhostLeaderboard(12L, request);
    }

    @DisplayName("없는 코스 고스트 리더보드 조회는 코스 없음 응답을 반환한다")
    @Test
    void returnsCourseNotFoundWhenCourseGhostLeaderboardCourseIsMissing() throws Exception {
        CourseGhostLeaderboardRequest request = new CourseGhostLeaderboardRequest(20, null);
        given(courseService.findCourseGhostLeaderboard(999L, request))
                .willThrow(new CustomException(ResponseCode.COURSE_NOT_FOUND));

        mockMvc.perform(get("/api/courses/{courseId}/ghost-leaderboard", 999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.COURSE_NOT_FOUND.getCode()));

        then(courseService).should().findCourseGhostLeaderboard(999L, request);
    }

    @DisplayName("코스 리더보드 조회에서 courseId가 없으면 잘못된 입력 응답을 반환한다")
    @Test
    void returnsInvalidInputWhenCourseLeaderboardCourseIdIsMissing() throws Exception {
        mockMvc.perform(get("/api/courses/leaderboards"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INPUT_VALUE.getCode()))
                .andExpect(jsonPath("$.data[0].field").value("courseId"));

        then(courseService).should(never()).findCourseLeaderboard(any());
    }

    @DisplayName("코스 리더보드 size가 유효하지 않으면 잘못된 입력 응답을 반환한다")
    @ParameterizedTest(name = "size={0}")
    @ValueSource(strings = {"0", "-1", "101", "abc", "1.5"})
    void returnsInvalidInputWhenCourseLeaderboardSizeIsInvalid(String invalidSize) throws Exception {
        mockMvc.perform(get("/api/courses/leaderboards")
                        .queryParam("courseId", "12")
                        .queryParam("size", invalidSize))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INPUT_VALUE.getCode()));

        then(courseService).should(never()).findCourseLeaderboard(any());
    }

    @DisplayName("주변 고스트 코스 조회에서 필수 쿼리 파라미터가 없으면 잘못된 입력 응답을 반환한다")
    @ParameterizedTest(name = "누락 파라미터: {0}")
    @ValueSource(strings = {"latitude", "longitude", "preferredDistanceMs"})
    void returnsInvalidInputValueWhenRequiredGhostQueryParameterIsMissing(String missingParameter) throws Exception {
        mockMvc.perform(getNearbyGhostCoursesWithout(missingParameter))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INPUT_VALUE.getCode()))
                .andExpect(jsonPath("$.data[0].field").value(missingParameter));

        then(courseService).should(never()).findNearbyGhostCourses(any());
    }

    @DisplayName("주변 고스트 코스 조회에서 위도 또는 경도가 범위를 벗어나면 잘못된 입력 응답을 반환한다")
    @ParameterizedTest(name = "잘못된 {0}: {1}")
    @CsvSource({
            "latitude,91.0",
            "latitude,-91.0",
            "longitude,181.0",
            "longitude,-181.0"
    })
    void returnsInvalidInputValueWhenGhostLatitudeOrLongitudeIsOutOfRange(String parameterName, String invalidValue) throws Exception {
        mockMvc.perform(getNearbyGhostCoursesWith(parameterName, invalidValue))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INPUT_VALUE.getCode()))
                .andExpect(jsonPath("$.data[0].field").value(parameterName));

        then(courseService).should(never()).findNearbyGhostCourses(any());
    }

    @DisplayName("주변 고스트 코스 조회에서 pageSize가 유효하지 않으면 잘못된 입력 응답을 반환한다")
    @ParameterizedTest(name = "pageSize={0}")
    @ValueSource(strings = {"0", "-1", "11", "abc", "1.5"})
    void returnsInvalidInputValueWhenGhostPageSizeIsInvalid(String invalidPageSize) throws Exception {
        mockMvc.perform(nearbyGhostCoursesRequest(
                        "37.5665",
                        "126.9780",
                        new String[][]{{"1", "3000"}, {"3001", "5000"}},
                        invalidPageSize,
                        null
                ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INPUT_VALUE.getCode()));

        then(courseService).should(never()).findNearbyGhostCourses(any());
    }

    @DisplayName("주변 고스트 코스 응답을 성공 응답 형식으로 감싼다")
    @Test
    void wrapsNearbyGhostCoursesResponseInSuccessEnvelope() throws Exception {
        NearbyGhostCoursesRequest request = new NearbyGhostCoursesRequest(
                37.5665,
                126.9780,
                List.of(
                        new PreferredDistanceRange(1, 3000),
                        new PreferredDistanceRange(3001, 5000)
                ),
                3,
                "bmV4dA"
        );
        NearbyGhostCoursesResponse response = new NearbyGhostCoursesResponse(
                List.of(new NearbyCourseItemResponse(
                        1L,
                        "Ghost Han River",
                        CourseStatus.OFFICIAL,
                        5000,
                        35,
                        37.57,
                        126.97,
                        37.58,
                        126.98,
                        240,
                        new NearbyCourseItemResponse.DifficultyOption("MEDIUM", "보통"),
                        List.of(
                                new NearbyCourseItemResponse.CourseTypeOption("RIVERSIDE", "강변"),
                                new NearbyCourseItemResponse.CourseTypeOption("PARK", "공원")
                        ),
                        14L,
                        8L,
                        "https://example.com/course.jpg"
                )),
                true,
                "next-cursor"
        );
        given(courseService.findNearbyGhostCourses(request)).willReturn(response);

        mockMvc.perform(nearbyGhostCoursesRequest(
                        "37.5665",
                        "126.9780",
                        new String[][]{{"1", "3000"}, {"3001", "5000"}},
                        "3",
                        "bmV4dA"
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.OK.getMessage()))
                .andExpect(jsonPath("$.data.items[0].courseId").value(1L))
                .andExpect(jsonPath("$.data.items[0].title").value("Ghost Han River"))
                .andExpect(jsonPath("$.data.items[0].status").value(CourseStatus.OFFICIAL.name()))
                .andExpect(jsonPath("$.data.items[0].difficulty.data").value("MEDIUM"))
                .andExpect(jsonPath("$.data.items[0].difficulty.label").value("보통"))
                .andExpect(jsonPath("$.data.items[0].courseTypes[0].data").value("RIVERSIDE"))
                .andExpect(jsonPath("$.data.items[0].courseTypes[0].label").value("강변"))
                .andExpect(jsonPath("$.data.items[0].likeCount").value(14L))
                .andExpect(jsonPath("$.data.items[0].bookmarkCount").value(8L))
                .andExpect(jsonPath("$.data.items[0].likeCount").value(14L))
                .andExpect(jsonPath("$.data.items[0].bookmarkCount").value(8L))
                .andExpect(jsonPath("$.data.hasMore").value(true))
                .andExpect(jsonPath("$.data.nextCursor").value("next-cursor"));

        then(courseService).should().findNearbyGhostCourses(request);
    }

    @DisplayName("주변 고스트 코스 조회에서 선호 거리 목록과 pageSize를 요청 DTO에 바인딩한다")
    @Test
    void bindsNearbyGhostCourseQueryParametersToRequestDto() throws Exception {
        NearbyGhostCoursesRequest request = new NearbyGhostCoursesRequest(
                37.5665,
                126.9780,
                List.of(
                        new PreferredDistanceRange(1, 3000),
                        new PreferredDistanceRange(3001, 5000),
                        new PreferredDistanceRange(5001, 10000)
                ),
                2,
                null
        );
        NearbyGhostCoursesResponse response = new NearbyGhostCoursesResponse(List.of(), false, null);
        given(courseService.findNearbyGhostCourses(request)).willReturn(response);

        mockMvc.perform(nearbyGhostCoursesRequest(
                        "37.5665",
                        "126.9780",
                        new String[][]{{"1", "3000"}, {"3001", "5000"}, {"5001", "10000"}},
                        "2",
                        null
                ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        then(courseService).should().findNearbyGhostCourses(request);
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder getNearbyCoursesWithout(
            String missingParameter
    ) {
        return nearbyCoursesRequest(
                "latitude".equals(missingParameter) ? null : "37.5665",
                "longitude".equals(missingParameter) ? null : "126.9780",
                "radiusM".equals(missingParameter) ? null : "1000",
                "targetDistanceM".equals(missingParameter) ? null : "10000",
                null,
                null
        );
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder getNearbyCoursesWith(
            String parameterName,
            String value
    ) {
        return nearbyCoursesRequest(
                "latitude".equals(parameterName) ? value : "37.5665",
                "longitude".equals(parameterName) ? value : "126.9780",
                "1000",
                "10000",
                null,
                null
        );
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder nearbyCoursesRequest(
            String latitude,
            String longitude,
            String radiusM,
            String targetDistanceM,
            String pageSize,
            String cursor
    ) {
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder requestBuilder = get("/api/courses/nearby");
        if (latitude != null) {
            requestBuilder.param("latitude", latitude);
        }
        if (longitude != null) {
            requestBuilder.param("longitude", longitude);
        }
        if (radiusM != null) {
            requestBuilder.param("radiusM", radiusM);
        }
        if (targetDistanceM != null) {
            requestBuilder.param("targetDistanceM", targetDistanceM);
        }
        if (pageSize != null) {
            requestBuilder.param("pageSize", pageSize);
        }
        if (cursor != null) {
            requestBuilder.param("cursor", cursor);
        }
        return requestBuilder;
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder getNearbyGhostCoursesWithout(
            String missingParameter
    ) {
        return nearbyGhostCoursesRequest(
                "latitude".equals(missingParameter) ? null : "37.5665",
                "longitude".equals(missingParameter) ? null : "126.9780",
                "preferredDistanceMs".equals(missingParameter) ? null : new String[][]{{"1", "3000"}, {"3001", "5000"}},
                null,
                null
        );
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder getNearbyGhostCoursesWith(
            String parameterName,
            String value
    ) {
        return nearbyGhostCoursesRequest(
                "latitude".equals(parameterName) ? value : "37.5665",
                "longitude".equals(parameterName) ? value : "126.9780",
                new String[][]{{"1", "3000"}, {"3001", "5000"}},
                null,
                null
        );
    }

    private static org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder nearbyGhostCoursesRequest(
            String latitude,
            String longitude,
            String[][] preferredDistanceMs,
            String pageSize,
            String cursor
    ) {
        org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder requestBuilder = get("/api/courses/nearby/ghost");
        if (latitude != null) {
            requestBuilder.param("latitude", latitude);
        }
        if (longitude != null) {
            requestBuilder.param("longitude", longitude);
        }
        if (preferredDistanceMs != null) {
            for (int index = 0; index < preferredDistanceMs.length; index++) {
                requestBuilder.param("preferredDistanceMs[" + index + "].min", preferredDistanceMs[index][0]);
                requestBuilder.param("preferredDistanceMs[" + index + "].max", preferredDistanceMs[index][1]);
            }
        }
        if (pageSize != null) {
            requestBuilder.param("pageSize", pageSize);
        }
        if (cursor != null) {
            requestBuilder.param("cursor", cursor);
        }
        return requestBuilder;
    }
}
