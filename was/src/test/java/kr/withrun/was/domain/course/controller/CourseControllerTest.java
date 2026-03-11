package kr.withrun.was.domain.course.controller;

import kr.withrun.was.domain.course.dto.NearbyCourseItemResponse;
import kr.withrun.was.domain.course.dto.NearbyCoursesRequest;
import kr.withrun.was.domain.course.dto.NearbyCoursesResponse;
import kr.withrun.was.domain.course.service.CourseService;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.global.common.type.Difficulty;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CourseController.class, properties = "spring.config.import=")
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
                        Difficulty.MEDIUM,
                        List.of(CourseType.RIVERSIDE, CourseType.PARK),
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
                .andExpect(jsonPath("$.data.items[0].courseTypes[0]").value(CourseType.RIVERSIDE.name()))
                .andExpect(jsonPath("$.data.hasMore").value(true))
                .andExpect(jsonPath("$.data.nextCursor").value("next-cursor"))
                .andExpect(jsonPath("$.data.suggestedNextRadiusM").value(2000));

        then(courseService).should().findNearbyCourses(request);
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
}
