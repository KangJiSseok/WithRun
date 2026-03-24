package kr.withrun.was.domain.course.service;

import kr.withrun.was.domain.course.dto.CreateCourseReviewRequest;
import kr.withrun.was.domain.course.dto.CreateCourseReviewResponse;
import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.entity.CourseReview;
import kr.withrun.was.domain.course.entity.CourseReviewCourseType;
import kr.withrun.was.domain.course.repository.CourseRepository;
import kr.withrun.was.domain.course.repository.CourseReviewCourseTypeRepository;
import kr.withrun.was.domain.course.repository.CourseReviewRepository;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.repository.UserRepository;
import kr.withrun.was.domain.user.type.Gender;
import kr.withrun.was.global.common.type.Difficulty;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("코스 리뷰 서비스")
class CourseReviewServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseReviewRepository courseReviewRepository;

    @Mock
    private CourseReviewCourseTypeRepository courseReviewCourseTypeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseSignalService courseSignalService;

    @InjectMocks
    private CourseReviewService courseReviewService;


    @DisplayName("코스 리뷰를 생성하면 평점, 난이도, 코스 타입을 저장하고 응답을 반환한다")
    @Test
    void createsCourseReviewAndReturnsSavedReview() {
        Course course = course(101L);
        User user = user(1L);
        CreateCourseReviewRequest request = new CreateCourseReviewRequest(1L, 5, List.of("RIVERSIDE", "URBAN"), "MEDIUM");
        CourseReview savedReview = review(501L, course, user, 5, Difficulty.MEDIUM, LocalDateTime.of(2026, 3, 8, 21, 0));

        when(courseRepository.findNotDeletedCourse(course.getId())).thenReturn(Optional.of(course));
        when(userRepository.findNotDeletedUser(user.getId())).thenReturn(Optional.of(user));
        when(courseReviewRepository.existsByCourseIdAndUserId(course.getId(), user.getId())).thenReturn(false);
        when(courseReviewRepository.save(any(CourseReview.class))).thenReturn(savedReview);

        CreateCourseReviewResponse response = courseReviewService.createCourseReview(course.getId(), request);

        assertThat(response.courseReviewId()).isEqualTo(savedReview.getId());
        assertThat(response.courseId()).isEqualTo(course.getId());
        assertThat(response.rating()).isEqualTo(5);
        assertThat(response.courseTypes()).containsExactly(
                new CreateCourseReviewResponse.CourseTypeOption("RIVERSIDE", "강변"),
                new CreateCourseReviewResponse.CourseTypeOption("URBAN", "도심")
        );
        assertThat(response.submittedDifficulty()).isEqualTo(new CreateCourseReviewResponse.DifficultyOption("MEDIUM", "보통"));
        assertThat(response.createdAt()).isEqualTo(savedReview.getCreatedAt());

        ArgumentCaptor<List<CourseReviewCourseType>> courseTypesCaptor = ArgumentCaptor.forClass(List.class);
        verify(courseReviewCourseTypeRepository).saveAll(courseTypesCaptor.capture());
        assertThat(courseTypesCaptor.getValue())
                .extracting(CourseReviewCourseType::getCourseType)
                .containsExactly(CourseType.RIVERSIDE, CourseType.URBAN);
    }

    @DisplayName("이미 작성한 리뷰가 있으면 REVIEW_ALREADY_EXISTS 예외를 던진다")
    @Test
    void throwsReviewAlreadyExistsWhenDuplicateReviewIsRequested() {
        Course course = course(102L);
        User user = user(2L);
        CreateCourseReviewRequest request = new CreateCourseReviewRequest(2L, 4, List.of("PARK"), "EASY");

        when(courseRepository.findNotDeletedCourse(course.getId())).thenReturn(Optional.of(course));
        when(userRepository.findNotDeletedUser(user.getId())).thenReturn(Optional.of(user));
        when(courseReviewRepository.existsByCourseIdAndUserId(course.getId(), user.getId())).thenReturn(true);

        assertThatThrownBy(() -> courseReviewService.createCourseReview(course.getId(), request))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.REVIEW_ALREADY_EXISTS);

        verify(courseReviewRepository, never()).save(any(CourseReview.class));
        verify(courseReviewCourseTypeRepository, never()).saveAll(any());
    }

    @DisplayName("리뷰 저장 중 유니크 제약 위반이 발생하면 REVIEW_ALREADY_EXISTS 예외로 변환한다")
    @Test
    void throwsReviewAlreadyExistsWhenUniqueConstraintIsViolatedDuringSave() {
        Course course = course(105L);
        User user = user(5L);
        CreateCourseReviewRequest request = new CreateCourseReviewRequest(5L, 4, List.of("PARK"), "EASY");

        when(courseRepository.findNotDeletedCourse(course.getId())).thenReturn(Optional.of(course));
        when(userRepository.findNotDeletedUser(user.getId())).thenReturn(Optional.of(user));
        when(courseReviewRepository.existsByCourseIdAndUserId(course.getId(), user.getId())).thenReturn(false);
        when(courseReviewRepository.save(any(CourseReview.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate review"));

        assertThatThrownBy(() -> courseReviewService.createCourseReview(course.getId(), request))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.REVIEW_ALREADY_EXISTS);

        verify(courseReviewCourseTypeRepository, never()).saveAll(any());
    }

    @DisplayName("유효하지 않은 코스 타입이 포함되면 잘못된 입력 예외를 던진다")
    @Test
    void throwsInvalidInputValueWhenCourseTypeIsInvalid() {
        CreateCourseReviewRequest request = new CreateCourseReviewRequest(3L, 5, List.of("RIVERSIDE", "BEACH"), "MEDIUM");

        assertThatThrownBy(() -> courseReviewService.createCourseReview(103L, request))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.INVALID_INPUT_VALUE);
    }

    @DisplayName("유효하지 않은 난이도가 들어오면 잘못된 입력 예외를 던진다")
    @Test
    void throwsInvalidInputValueWhenDifficultyIsInvalid() {
        CreateCourseReviewRequest request = new CreateCourseReviewRequest(4L, 5, List.of("RIVERSIDE"), "VERY_HARD");

        assertThatThrownBy(() -> courseReviewService.createCourseReview(104L, request))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.INVALID_INPUT_VALUE);
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
                .coordinates(new Coordinates(List.of(37.5665), List.of(126.9780), List.of(12.0)))
                .build();
        setField(course, "id", id);
        return course;
    }

    private User user(Long id) {
        User user = instantiateUser();
        setField(user, "id", id);
        setField(user, "nickname", "runner-" + id);
        setField(user, "birthDate", LocalDate.of(1995, 3, 11));
        setField(user, "gender", Gender.MALE);
        setField(user, "height", 175.0);
        setField(user, "weight", 68.0);
        return user;
    }

    private CourseReview review(
            Long id,
            Course course,
            User user,
            int rating,
            Difficulty difficulty,
            LocalDateTime createdAt
    ) {
        CourseReview review = CourseReview.create(course, user, rating, difficulty);
        setField(review, "id", id);
        setField(review, "createdAt", createdAt);
        return review;
    }

    private User instantiateUser() {
        try {
            Constructor<User> constructor = User.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to instantiate user", exception);
        }
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
