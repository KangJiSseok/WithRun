package kr.withrun.was.domain.course.service;

import kr.withrun.was.domain.course.dto.CourseDetailResponse;
import kr.withrun.was.domain.course.dto.CourseSurveyResponse;
import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.repository.CourseDifficultyRepository;
import kr.withrun.was.domain.course.repository.CourseLikeRepository;
import kr.withrun.was.domain.course.repository.CourseRepository;
import kr.withrun.was.domain.course.repository.CourseReviewRepository;
import kr.withrun.was.domain.course.repository.CourseTypeMapRepository;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.global.common.type.Difficulty;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("코스 서비스")
class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseDifficultyRepository courseDifficultyRepository;

    @Mock
    private CourseTypeMapRepository courseTypeMapRepository;

    @Mock
    private CourseLikeRepository courseLikeRepository;

    @Mock
    private CourseReviewRepository courseReviewRepository;

    private CourseService courseService;

    @BeforeEach
    void setUp() {
        courseService = new CourseService(
                courseRepository,
                courseDifficultyRepository,
                courseTypeMapRepository,
                courseLikeRepository,
                courseReviewRepository
        );
    }

    @DisplayName("코스 상세 조회는 좌표와 메타 정보를 함께 반환한다")
    @Test
    void returnsCourseDetailIncludingCoordinates() {
        Course course = course(1L);
        when(courseRepository.findNotDeletedCourse(course.getId())).thenReturn(Optional.of(course));
        when(courseDifficultyRepository.findDifficultyByCourseId(course.getId())).thenReturn(Optional.of(Difficulty.EASY));
        when(courseTypeMapRepository.findCourseTypesByCourseId(course.getId()))
                .thenReturn(List.of(CourseType.RIVERSIDE, CourseType.PARK));
        when(courseLikeRepository.countByCourseId(course.getId())).thenReturn(14L);
        when(courseReviewRepository.findAverageRatingByCourseId(course.getId())).thenReturn(4.5);

        CourseDetailResponse response = courseService.findCourseDetail(course.getId());

        assertThat(response.courseId()).isEqualTo(course.getId());
        assertThat(response.title()).isEqualTo(course.getTitle());
        assertThat(response.status()).isEqualTo(course.getStatus());
        assertThat(response.difficulty()).isEqualTo(Difficulty.EASY);
        assertThat(response.coordinates()).isEqualTo(course.getCoordinates());
        assertThat(response.types()).containsExactly(CourseType.RIVERSIDE, CourseType.PARK);
        assertThat(response.likeCount()).isEqualTo(14L);
        assertThat(response.isLiked()).isFalse();
        assertThat(response.isBookmarked()).isFalse();
        assertThat(response.averageRating()).isEqualTo(4.5);
    }

    @DisplayName("코스 조회용 enum 목록을 index와 label로 반환한다")
    @Test
    void returnsCourseLookups() {
        CourseSurveyResponse response = courseService.findCourseSurveys();

        assertThat(response.courseTypes()).containsExactly(
                new CourseSurveyResponse.CourseSurveyListResponse(1, "RIVERSIDE"),
                new CourseSurveyResponse.CourseSurveyListResponse(2, "PARK"),
                new CourseSurveyResponse.CourseSurveyListResponse(3, "MOUNTAIN_TRAIL"),
                new CourseSurveyResponse.CourseSurveyListResponse(4, "TRACK"),
                new CourseSurveyResponse.CourseSurveyListResponse(5, "URBAN"),
                new CourseSurveyResponse.CourseSurveyListResponse(6, "OTHER")
        );
        assertThat(response.difficulties()).containsExactly(
                new CourseSurveyResponse.CourseSurveyListResponse(1, "EASY"),
                new CourseSurveyResponse.CourseSurveyListResponse(2, "MEDIUM"),
                new CourseSurveyResponse.CourseSurveyListResponse(3, "HARD")
        );
    }

    @DisplayName("없는 코스 상세 조회는 코스 없음 예외를 던진다")
    @Test
    void throwsCourseNotFoundWhenCourseIsMissing() {
        when(courseRepository.findNotDeletedCourse(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.findCourseDetail(99L))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.COURSE_NOT_FOUND);
    }

    private Course course(Long id) {
        Course course = Course.builder()
                .title("Course " + id)
                .status(CourseStatus.OFFICIAL)
                .distanceM(5000)
                .elevationGainM(120)
                .snapshotImageUrl("snapshot-" + id)
                .startLatitude(37.5665)
                .startLongitude(126.9780)
                .endLatitude(37.5700)
                .endLongitude(126.9820)
                .coordinates(new Coordinates(List.of(37.5665, 37.5700), List.of(126.9780, 126.9820), List.of(12.0, 15.0)))
                .build();
        setField(course, "id", id);
        return course;
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
