package kr.withrun.was.domain.course.controller;

import java.time.LocalDateTime;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import kr.withrun.was.domain.course.dto.CourseBookmarkAddResponse;
import kr.withrun.was.domain.course.dto.CourseBookmarkRemoveResponse;
import kr.withrun.was.domain.course.service.CourseBookmarkService;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CourseBookmarkController.class, properties = "spring.config.import=")
@DisplayName("코스 북마크 컨트롤러")
class CourseBookmarkControllerTest {

    static {
        System.setProperty("spring.cloud.aws.parameterstore.enabled", "false");
        System.setProperty("spring.config.import", "");
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseBookmarkService courseBookmarkService;

    @AfterAll
    static void clearConfigOverrides() {
        System.clearProperty("spring.cloud.aws.parameterstore.enabled");
        System.clearProperty("spring.config.import");
    }

    @DisplayName("북마크 추가 응답을 성공 응답 포맷으로 감싼다")
    @Test
    void wrapsAddBookmarkResponseInSuccessEnvelope() throws Exception {
        long courseId = 10L;
        long userId = 20L;
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 12, 16, 0);
        CourseBookmarkAddResponse response = new CourseBookmarkAddResponse(courseId, true, createdAt);
        given(courseBookmarkService.addBookmark(courseId, userId)).willReturn(response);

        mockMvc.perform(post("/api/courses/{courseId}/bookmark/{userId}", courseId, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.OK.getMessage()))
                .andExpect(jsonPath("$.data.courseId").value(courseId))
                .andExpect(jsonPath("$.data.isBookmarked").value(true))
                .andExpect(jsonPath("$.data.createdAt").value("2026-03-12T16:00:00"));

        then(courseBookmarkService).should().addBookmark(courseId, userId);
    }

    @DisplayName("북마크 삭제 응답을 성공 응답 포맷으로 감싼다")
    @Test
    void wrapsRemoveBookmarkResponseInSuccessEnvelope() throws Exception {
        long courseId = 11L;
        long userId = 21L;
        CourseBookmarkRemoveResponse response = new CourseBookmarkRemoveResponse(courseId, false);
        given(courseBookmarkService.removeBookmark(courseId, userId)).willReturn(response);

        mockMvc.perform(delete("/api/courses/{courseId}/bookmark/{userId}", courseId, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.OK.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.OK.getMessage()))
                .andExpect(jsonPath("$.data.courseId").value(courseId))
                .andExpect(jsonPath("$.data.isBookmarked").value(false));

        then(courseBookmarkService).should().removeBookmark(courseId, userId);
    }

    @DisplayName("없는 코스에 북마크를 추가하면 코스 없음 응답을 반환한다")
    @Test
    void returnsCourseNotFoundWhenAddingBookmarkToMissingCourse() throws Exception {
        long courseId = 999L;
        long userId = 20L;
        given(courseBookmarkService.addBookmark(courseId, userId))
                .willThrow(new CustomException(ResponseCode.COURSE_NOT_FOUND));

        mockMvc.perform(post("/api/courses/{courseId}/bookmark/{userId}", courseId, userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.COURSE_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.COURSE_NOT_FOUND.getMessage()));

        then(courseBookmarkService).should().addBookmark(courseId, userId);
    }

    @DisplayName("없는 사용자가 북마크를 삭제하면 사용자 없음 응답을 반환한다")
    @Test
    void returnsUserNotFoundWhenRemovingBookmarkForMissingUser() throws Exception {
        long courseId = 12L;
        long userId = 999L;
        given(courseBookmarkService.removeBookmark(courseId, userId))
                .willThrow(new CustomException(ResponseCode.USER_NOT_FOUND));

        mockMvc.perform(delete("/api/courses/{courseId}/bookmark/{userId}", courseId, userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.USER_NOT_FOUND.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.USER_NOT_FOUND.getMessage()));

        then(courseBookmarkService).should().removeBookmark(courseId, userId);
    }
}
