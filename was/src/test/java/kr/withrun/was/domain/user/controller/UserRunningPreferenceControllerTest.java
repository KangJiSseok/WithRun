package kr.withrun.was.domain.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.withrun.was.domain.auth.jwt.JwtProvider;
import kr.withrun.was.domain.user.dto.UpdateUserRunningPreferenceRequest;
import kr.withrun.was.domain.user.dto.UserRunningPreferenceResponse;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.service.UserRunningPreferenceService;
import kr.withrun.was.domain.user.type.Gender;
import kr.withrun.was.domain.user.type.Purpose;
import kr.withrun.was.global.common.type.Difficulty;
import kr.withrun.was.global.common.type.TimeSlot;
import kr.withrun.was.global.response.ResponseCode;
import kr.withrun.was.domain.course.type.CourseType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        properties = {
                "spring.config.import=",
                "spring.cloud.aws.parameterstore.enabled=false"
        }
)
@AutoConfigureMockMvc
@DisplayName("UserRunningPreferenceController")
class UserRunningPreferenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private UserRunningPreferenceService userRunningPreferenceService;

    @Test
    @DisplayName("유효한 access token이면 현재 러닝 기본 설정을 반환한다")
    void returnsRunningPreferenceWhenTokenIsValid() throws Exception {
        given(userRunningPreferenceService.getRunningPreference(10L)).willReturn(
                new UserRunningPreferenceResponse(
                        Purpose.DIET,
                        TimeSlot.MORNING,
                        5.0,
                        Difficulty.MEDIUM,
                        CourseType.PARK
                )
        );

        mockMvc.perform(get("/api/users/me/running-preferences")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(10L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.purpose").value("DIET"))
                .andExpect(jsonPath("$.data.timeSlot").value("MORNING"))
                .andExpect(jsonPath("$.data.preferredDistanceKm").value(5.0))
                .andExpect(jsonPath("$.data.preferredDifficulty").value("MEDIUM"))
                .andExpect(jsonPath("$.data.courseType").value("PARK"));

        then(userRunningPreferenceService).should().getRunningPreference(10L);
    }

    @Test
    @DisplayName("유효한 access token이면 현재 사용자 러닝 기본 설정 저장을 위임한다")
    void updatesRunningPreferenceWhenTokenIsValid() throws Exception {
        UpdateUserRunningPreferenceRequest request = new UpdateUserRunningPreferenceRequest(
                Purpose.RACE_PREPARATION,
                TimeSlot.EVENING,
                10.0,
                Difficulty.HARD,
                CourseType.TRACK
        );

        mockMvc.perform(put("/api/users/me/running-preferences")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(10L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()));

        then(userRunningPreferenceService).should().updateRunningPreference(eq(10L), eq(request));
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 인증 필요 응답을 반환한다")
    void returnsUnauthorizedWhenAuthorizationHeaderIsMissing() throws Exception {
        mockMvc.perform(get("/api/users/me/running-preferences"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.UNAUTHORIZED.getCode()));

        then(userRunningPreferenceService).should(never()).getRunningPreference(any());
    }

    private String bearerToken(Long userId) {
        return "Bearer " + jwtProvider.generateTokenPair(completedUser(userId), "google").accessToken();
    }

    private User completedUser(Long id) {
        User user = User.createPendingSocialUser();
        setField(user, "id", id);
        user.completeProfile(
                "runner",
                LocalDate.of(1999, 1, 2),
                Gender.MALE,
                180.0,
                72.5
        );
        return user;
    }

    private void setField(Object target, String fieldName, Object value) {
        Class<?> currentClass = target.getClass();
        while (currentClass != null) {
            try {
                Field field = currentClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException exception) {
                currentClass = currentClass.getSuperclass();
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException("Failed to set field " + fieldName, exception);
            }
        }

        throw new IllegalArgumentException("Field not found: " + fieldName);
    }
}
