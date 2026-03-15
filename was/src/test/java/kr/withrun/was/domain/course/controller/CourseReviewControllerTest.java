package kr.withrun.was.domain.course.controller;

import kr.withrun.was.domain.course.dto.CreateCourseReviewRequest;
import kr.withrun.was.domain.course.dto.CreateCourseReviewResponse;
import kr.withrun.was.domain.course.service.CourseReviewService;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.global.common.type.Difficulty;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@WebMvcTest(
        controllers = CourseReviewController.class,
        properties = {
                "spring.cloud.aws.parameterstore.enabled=false",
                "spring.config.import="
        }
)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("코스 리뷰 컨트롤러")
class CourseReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CourseReviewService courseReviewService;

    @DisplayName("코스 리뷰 생성 응답을 생성 성공 포맷으로 감싼다")
    @Test
    void wrapsCreateCourseReviewResponseInCreatedEnvelope() throws Exception {
        long courseId = 101L;
        CreateCourseReviewRequest request = validRequest();
        CreateCourseReviewResponse response = new CreateCourseReviewResponse(
                501L,
                courseId,
                5,
                List.of(CourseType.RIVERSIDE, CourseType.URBAN),
                Difficulty.MEDIUM,
                LocalDateTime.of(2026, 3, 8, 21, 0)
        );
        given(courseReviewService.createCourseReview(courseId, request)).willReturn(response);

        mockMvc.perform(post("/api/courses/{courseId}/reviews", courseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(ResponseCode.CREATED.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.CREATED.getMessage()))
                .andExpect(jsonPath("$.data.courseReviewId").value(501L))
                .andExpect(jsonPath("$.data.courseId").value(courseId))
                .andExpect(jsonPath("$.data.rating").value(5))
                .andExpect(jsonPath("$.data.courseTypes[0]").value(CourseType.RIVERSIDE.name()))
                .andExpect(jsonPath("$.data.courseTypes[1]").value(CourseType.URBAN.name()))
                .andExpect(jsonPath("$.data.submittedDifficulty").value(Difficulty.MEDIUM.name()))
                .andExpect(jsonPath("$.data.createdAt").value("2026-03-08T21:00:00"));

        then(courseReviewService).should().createCourseReview(courseId, request);
    }

    @DisplayName("필수 요청 필드가 없으면 잘못된 입력 응답을 반환한다")
    @Test
    void returnsInvalidInputValueWhenRequiredFieldIsMissing() throws Exception {
        mockMvc.perform(post("/api/courses/{courseId}/reviews", 101L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 1,
                                  "courseTypes": ["RIVERSIDE"],
                                  "submittedDifficulty": "MEDIUM"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.INVALID_INPUT_VALUE.getCode()))
                .andExpect(jsonPath("$.data[0].field").value("rating"));

        then(courseReviewService).shouldHaveNoInteractions();
    }

    @DisplayName("이미 작성한 리뷰면 충돌 응답을 반환한다")
    @Test
    void returnsConflictWhenReviewAlreadyExists() throws Exception {
        long courseId = 101L;
        CreateCourseReviewRequest request = validRequest();
        given(courseReviewService.createCourseReview(courseId, request))
                .willThrow(new CustomException(ResponseCode.REVIEW_ALREADY_EXISTS));

        mockMvc.perform(post("/api/courses/{courseId}/reviews", courseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequestBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(ResponseCode.REVIEW_ALREADY_EXISTS.getCode()))
                .andExpect(jsonPath("$.message").value(ResponseCode.REVIEW_ALREADY_EXISTS.getMessage()));

        then(courseReviewService).should().createCourseReview(courseId, request);
    }

    private static CreateCourseReviewRequest validRequest() {
        return new CreateCourseReviewRequest(1L, 5, List.of("RIVERSIDE", "URBAN"), "MEDIUM");
    }

    private static String validRequestBody() {
        return """
                {
                  "userId": 1,
                  "rating": 5,
                  "courseTypes": ["RIVERSIDE", "URBAN"],
                  "submittedDifficulty": "MEDIUM"
                }
                """;
    }
}
