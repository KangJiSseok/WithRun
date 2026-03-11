package kr.withrun.was.domain.course.service;

import kr.withrun.was.domain.course.dto.CourseLikeStatusResponse;
import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.entity.CourseLike;
import kr.withrun.was.domain.course.repository.CourseLikeRepository;
import kr.withrun.was.domain.course.repository.CourseRepository;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.repository.UserRepository;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("코스 좋아요 서비스")
class CourseLikeServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseLikeRepository courseLikeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private User user;

    private CourseLikeService courseLikeService;

    @BeforeEach
    void setUp() {
        courseLikeService = new CourseLikeService(courseRepository, courseLikeRepository, userRepository);
    }

    @DisplayName("서비스는 클래스 기본 readOnly와 쓰기 메서드 트랜잭션을 함께 사용한다")
    @Test
    void usesReadOnlyAtClassLevelAndWritableMethods() throws NoSuchMethodException {
        Transactional classTransactional = CourseLikeService.class.getAnnotation(Transactional.class);
        Transactional likeTransactional = CourseLikeService.class
                .getDeclaredMethod("likeCourse", Long.class, Long.class)
                .getAnnotation(Transactional.class);
        Transactional unlikeTransactional = CourseLikeService.class
                .getDeclaredMethod("unlikeCourse", Long.class, Long.class)
                .getAnnotation(Transactional.class);

        assertThat(classTransactional).isNotNull();
        assertThat(classTransactional.readOnly()).isTrue();
        assertThat(likeTransactional).isNotNull();
        assertThat(likeTransactional.readOnly()).isFalse();
        assertThat(unlikeTransactional).isNotNull();
        assertThat(unlikeTransactional.readOnly()).isFalse();
    }

    @DisplayName("좋아요 이력이 없으면 새 좋아요를 생성한다")
    @Test
    void createsNewLikeWhenHistoryDoesNotExist() {
        Course course = communityCourse();
        givenActiveCourseAndUser(course);
        given(courseLikeRepository.findByCourseIdAndUserId(1L, 2L)).willReturn(Optional.empty());
        given(courseLikeRepository.save(any(CourseLike.class))).willAnswer(invocation -> invocation.getArgument(0));
        given(courseLikeRepository.countActiveLikedByCourseId(1L)).willReturn(1L);

        CourseLikeStatusResponse response = courseLikeService.likeCourse(1L, 2L);

        assertThat(response).isEqualTo(new CourseLikeStatusResponse(1L, true, 1L, CourseStatus.COMMUNITY));
        verify(courseLikeRepository).save(any(CourseLike.class));
    }

    @DisplayName("좋아요가 false인 기존 레코드는 true로 복구하고 기준 수를 넘기면 코스를 공식 코스로 승격한다")
    @Test
    void reactivatesExistingLikeAndPromotesCommunityCourse() {
        Course course = communityCourse();
        CourseLike courseLike = CourseLike.create(course, user, false);

        givenActiveCourseAndUser(course);
        given(courseLikeRepository.findByCourseIdAndUserId(1L, 2L)).willReturn(Optional.of(courseLike));
        given(courseLikeRepository.countActiveLikedByCourseId(1L)).willReturn(10L);

        CourseLikeStatusResponse response = courseLikeService.likeCourse(1L, 2L);

        assertThat(response).isEqualTo(new CourseLikeStatusResponse(1L, true, 10L, CourseStatus.OFFICIAL));
        assertThat(courseLike.getIsLiked()).isTrue();
        assertThat(course.getStatus()).isEqualTo(CourseStatus.OFFICIAL);
        assertThat(course.getPromotedAt()).isNotNull();
        verify(courseLikeRepository, never()).save(any(CourseLike.class));
    }

    @DisplayName("이미 좋아요 상태여도 추가 레코드 없이 멱등하게 true를 유지한다")
    @Test
    void likeIsIdempotentWhenAlreadyLiked() {
        Course course = communityCourse();
        CourseLike courseLike = CourseLike.create(course, user, true);

        givenActiveCourseAndUser(course);
        given(courseLikeRepository.findByCourseIdAndUserId(1L, 2L)).willReturn(Optional.of(courseLike));
        given(courseLikeRepository.countActiveLikedByCourseId(1L)).willReturn(3L);

        CourseLikeStatusResponse response = courseLikeService.likeCourse(1L, 2L);

        assertThat(response).isEqualTo(new CourseLikeStatusResponse(1L, true, 3L, CourseStatus.COMMUNITY));
        assertThat(courseLike.getIsLiked()).isTrue();
        verify(courseLikeRepository, never()).save(any(CourseLike.class));
    }

    @DisplayName("좋아요 이력이 없으면 취소 요청에도 레코드를 만들지 않고 멱등하게 false를 반환한다")
    @Test
    void unlikeIsIdempotentWhenHistoryDoesNotExist() {
        Course course = communityCourse();
        givenActiveCourseAndUser(course);
        given(courseLikeRepository.findByCourseIdAndUserId(1L, 2L)).willReturn(Optional.empty());
        given(courseLikeRepository.countActiveLikedByCourseId(1L)).willReturn(0L);

        CourseLikeStatusResponse response = courseLikeService.unlikeCourse(1L, 2L);

        assertThat(response).isEqualTo(new CourseLikeStatusResponse(1L, false, 0L, CourseStatus.COMMUNITY));
        verify(courseLikeRepository, never()).save(any(CourseLike.class));
    }

    @DisplayName("좋아요 취소는 활성 좋아요를 false로 바꾸되 공식 코스를 강등하지 않는다")
    @Test
    void deactivatesLikeWithoutDemotingOfficialCourse() {
        LocalDateTime promotedAt = LocalDateTime.of(2026, 3, 11, 18, 0);
        Course course = Course.builder()
                .title("Official Course")
                .status(CourseStatus.OFFICIAL)
                .distanceM(10000)
                .elevationGainM(200)
                .snapshotImageUrl("snapshot")
                .startLatitude(37.5)
                .startLongitude(127.0)
                .endLatitude(37.6)
                .endLongitude(127.1)
                .coordinates(null)
                .promotedAt(promotedAt)
                .build();
        CourseLike courseLike = CourseLike.create(course, user, true);

        givenActiveCourseAndUser(course);
        given(courseLikeRepository.findByCourseIdAndUserId(1L, 2L)).willReturn(Optional.of(courseLike));
        given(courseLikeRepository.countActiveLikedByCourseId(1L)).willReturn(9L);

        CourseLikeStatusResponse response = courseLikeService.unlikeCourse(1L, 2L);

        assertThat(response).isEqualTo(new CourseLikeStatusResponse(1L, false, 9L, CourseStatus.OFFICIAL));
        assertThat(courseLike.getIsLiked()).isFalse();
        assertThat(course.getStatus()).isEqualTo(CourseStatus.OFFICIAL);
        assertThat(course.getPromotedAt()).isEqualTo(promotedAt);
    }

    @DisplayName("삭제되었거나 없는 코스에는 좋아요를 등록할 수 없다")
    @Test
    void throwsCourseNotFoundWhenCourseIsMissing() {
        given(courseRepository.findActiveById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> courseLikeService.likeCourse(1L, 2L))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.COURSE_NOT_FOUND);

        verify(userRepository, never()).findActiveUserById(any());
        verify(courseLikeRepository, never()).findByCourseIdAndUserId(eq(1L), eq(2L));
    }

    private void givenActiveCourseAndUser(Course course) {
        given(courseRepository.findActiveById(1L)).willReturn(Optional.of(course));
        given(userRepository.findActiveUserById(2L)).willReturn(Optional.of(user));
    }

    private Course communityCourse() {
        return Course.builder()
                .title("Community Course")
                .status(CourseStatus.COMMUNITY)
                .distanceM(8000)
                .elevationGainM(120)
                .snapshotImageUrl("snapshot")
                .startLatitude(37.5)
                .startLongitude(127.0)
                .endLatitude(37.6)
                .endLongitude(127.1)
                .coordinates(null)
                .build();
    }
}
