package kr.withrun.was.domain.course.service;

import kr.withrun.was.domain.course.dto.CourseDetailResponse;
import kr.withrun.was.domain.course.dto.CourseGhostDetailResponse;
import kr.withrun.was.domain.course.dto.CourseGhostLeaderboardRequest;
import kr.withrun.was.domain.course.dto.CourseGhostLeaderboardResponse;
import kr.withrun.was.domain.course.dto.CourseLeaderboardRequest;
import kr.withrun.was.domain.course.dto.CourseLeaderboardResponse;
import kr.withrun.was.domain.course.dto.CourseSurveyResponse;
import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.entity.CourseGhostLeaderboard;
import kr.withrun.was.domain.course.repository.CourseDifficultyRepository;
import kr.withrun.was.domain.course.repository.CourseGhostLeaderboardRepository;
import kr.withrun.was.domain.course.repository.CourseLikeRepository;
import kr.withrun.was.domain.course.repository.CourseRepository;
import kr.withrun.was.domain.course.repository.CourseReviewRepository;
import kr.withrun.was.domain.course.repository.CourseTypeMapRepository;
import kr.withrun.was.domain.course.repository.query.dto.CourseGhostLeaderboardRankRow;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.domain.course.util.CourseGhostLeaderboardCursorCodec;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.domain.running.entity.RunningSession;
import kr.withrun.was.domain.running.type.RunningMode;
import kr.withrun.was.domain.user.entity.User;
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
import java.time.LocalDateTime;
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

    @Mock
    private CourseGhostLeaderboardRepository courseGhostLeaderboardRepository;

    private CourseService courseService;

    @BeforeEach
    void setUp() {
        courseService = new CourseService(
                courseRepository,
                courseDifficultyRepository,
                courseTypeMapRepository,
                courseLikeRepository,
                courseReviewRepository,
                courseGhostLeaderboardRepository
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
        assertThat(response.difficulty()).isEqualTo(new CourseDetailResponse.DifficultyOption("EASY", "쉬움"));
        assertThat(response.coordinates()).isEqualTo(course.getCoordinates());
        assertThat(response.courseTypes()).containsExactly(
                new CourseDetailResponse.CourseTypeOption("RIVERSIDE", "강변"),
                new CourseDetailResponse.CourseTypeOption("PARK", "공원")
        );
        assertThat(response.likeCount()).isEqualTo(14L);
        assertThat(response.isLiked()).isFalse();
        assertThat(response.isBookmarked()).isFalse();
        assertThat(response.averageRating()).isEqualTo(4.5);
    }

    @DisplayName("코스 조회용 enum 목록을 data와 label로 반환한다")
    @Test
    void returnsCourseLookups() {
        CourseSurveyResponse response = courseService.findCourseSurveys();

        assertThat(response.courseTypes()).containsExactly(
                new CourseSurveyResponse.CourseTypeOption("RIVERSIDE", "강변"),
                new CourseSurveyResponse.CourseTypeOption("PARK", "공원"),
                new CourseSurveyResponse.CourseTypeOption("MOUNTAIN_TRAIL", "산악"),
                new CourseSurveyResponse.CourseTypeOption("TRACK", "트랙"),
                new CourseSurveyResponse.CourseTypeOption("URBAN", "도심"),
                new CourseSurveyResponse.CourseTypeOption("OTHER", "기타")
        );
        assertThat(response.difficulties()).containsExactly(
                new CourseSurveyResponse.DifficultyOption("EASY", "쉬움"),
                new CourseSurveyResponse.DifficultyOption("MEDIUM", "보통"),
                new CourseSurveyResponse.DifficultyOption("HARD", "어려움")
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

    @DisplayName("코스 고스트 상세 조회는 코스 상세 정보와 고스트 개인 기록 슬롯을 함께 반환한다")
    @Test
    void returnsCourseGhostDetailIncludingPersonalRecordSlot() {
        Course course = course(1L);
        long userId = 7L;
        User user = user(userId, "runner-a");
        RunningSession runningSession = RunningSession.start(user, RunningMode.COURSE, course, null, 37.5665, 126.9780);
        setField(runningSession, "id", 2002L);
        setField(runningSession, "durationSec", 1320);
        CourseGhostLeaderboard myRecord = CourseGhostLeaderboard.create(user, course, runningSession, 980);
        setField(myRecord, "id", 205L);
        setField(myRecord, "createdAt", LocalDateTime.of(2026, 3, 20, 7, 10));

        when(courseRepository.findNotDeletedCourse(course.getId())).thenReturn(Optional.of(course));
        when(courseDifficultyRepository.findDifficultyByCourseId(course.getId())).thenReturn(Optional.of(Difficulty.EASY));
        when(courseTypeMapRepository.findCourseTypesByCourseId(course.getId()))
                .thenReturn(List.of(CourseType.RIVERSIDE, CourseType.PARK));
        when(courseLikeRepository.countByCourseId(course.getId())).thenReturn(14L);
        when(courseReviewRepository.findAverageRatingByCourseId(course.getId())).thenReturn(4.5);
        when(courseGhostLeaderboardRepository.findTopByUserIdAndCourseId(userId, course.getId())).thenReturn(Optional.of(myRecord));
        when(courseGhostLeaderboardRepository.findRankedRowsByCourseId(course.getId())).thenReturn(List.of(
                new CourseGhostLeaderboardRankRow(101L, 3L, "runner-x", 1001L, 1200, 1L, LocalDateTime.of(2026, 3, 19, 9, 0)),
                new CourseGhostLeaderboardRankRow(205L, userId, "runner-a", 2002L, 980, 14L, LocalDateTime.of(2026, 3, 20, 7, 10))
        ));

        CourseGhostDetailResponse response = courseService.findCourseGhostDetail(course.getId(), userId);

        assertThat(response.courseId()).isEqualTo(course.getId());
        assertThat(response.title()).isEqualTo(course.getTitle());
        assertThat(response.difficulty()).isEqualTo(new CourseGhostDetailResponse.DifficultyOption("EASY", "쉬움"));
        assertThat(response.courseTypes()).containsExactly(
                new CourseGhostDetailResponse.CourseTypeOption("RIVERSIDE", "강변"),
                new CourseGhostDetailResponse.CourseTypeOption("PARK", "공원")
        );
        assertThat(response.likeCount()).isEqualTo(14L);
        assertThat(response.averageRating()).isEqualTo(4.5);
        assertThat(response.myRecord()).isEqualTo(new CourseGhostDetailResponse.MyRecord(
                205L,
                2002L,
                1320,
                980,
                14L,
                LocalDateTime.of(2026, 3, 20, 7, 10)
        ));
    }

    @DisplayName("코스 고스트 리더보드는 커서 기준으로 다음 페이지를 잘라 반환한다")
    @Test
    void returnsCourseGhostLeaderboardWithCursorPagination() {
        long courseId = 12L;
        Course course = course(courseId);
        CourseGhostLeaderboardRequest request = new CourseGhostLeaderboardRequest(2, null);
        List<CourseGhostLeaderboardRankRow> rows = List.of(
                new CourseGhostLeaderboardRankRow(101L, 7L, "runner-a", 1001L, 1250, 1L, LocalDateTime.of(2026, 3, 21, 10, 30)),
                new CourseGhostLeaderboardRankRow(102L, 8L, "runner-b", 1002L, 1250, 1L, LocalDateTime.of(2026, 3, 21, 10, 31)),
                new CourseGhostLeaderboardRankRow(205L, 9L, "runner-c", 2002L, 980, 14L, LocalDateTime.of(2026, 3, 20, 7, 10))
        );

        when(courseRepository.findNotDeletedCourse(courseId)).thenReturn(Optional.of(course));
        when(courseGhostLeaderboardRepository.findRankedRowsByCourseId(courseId)).thenReturn(rows);

        CourseGhostLeaderboardResponse response = courseService.findCourseGhostLeaderboard(courseId, request);

        assertThat(response.items()).hasSize(2);
        assertThat(response.items().getFirst().leaderboardId()).isEqualTo(101L);
        assertThat(response.items().get(1).leaderboardId()).isEqualTo(102L);
        assertThat(response.hasMore()).isTrue();
        assertThat(response.nextCursor()).isEqualTo(CourseGhostLeaderboardCursorCodec.encode(1250, 102L));
    }

    @DisplayName("코스 고스트 리더보드는 커서 이후 항목부터 이어서 반환한다")
    @Test
    void returnsCourseGhostLeaderboardAfterCursor() {
        long courseId = 12L;
        Course course = course(courseId);
        List<CourseGhostLeaderboardRankRow> rows = List.of(
                new CourseGhostLeaderboardRankRow(101L, 7L, "runner-a", 1001L, 1250, 1L, LocalDateTime.of(2026, 3, 21, 10, 30)),
                new CourseGhostLeaderboardRankRow(102L, 8L, "runner-b", 1002L, 1250, 1L, LocalDateTime.of(2026, 3, 21, 10, 31)),
                new CourseGhostLeaderboardRankRow(205L, 9L, "runner-c", 2002L, 980, 14L, LocalDateTime.of(2026, 3, 20, 7, 10))
        );
        CourseGhostLeaderboardRequest request = new CourseGhostLeaderboardRequest(
                2,
                CourseGhostLeaderboardCursorCodec.encode(1250, 102L)
        );

        when(courseRepository.findNotDeletedCourse(courseId)).thenReturn(Optional.of(course));
        when(courseGhostLeaderboardRepository.findRankedRowsByCourseId(courseId)).thenReturn(rows);

        CourseGhostLeaderboardResponse response = courseService.findCourseGhostLeaderboard(courseId, request);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().leaderboardId()).isEqualTo(205L);
        assertThat(response.hasMore()).isFalse();
        assertThat(response.nextCursor()).isNull();
    }

    @DisplayName("잘못된 코스 고스트 리더보드 커서는 예외를 던진다")
    @Test
    void throwsInvalidCursorWhenCourseGhostLeaderboardCursorIsMalformed() {
        long courseId = 12L;
        Course course = course(courseId);
        when(courseRepository.findNotDeletedCourse(courseId)).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> courseService.findCourseGhostLeaderboard(courseId, new CourseGhostLeaderboardRequest(20, "%%%")))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.INVALID_CURSOR);
    }

    @DisplayName("없는 코스의 고스트 리더보드 조회는 코스 없음 예외를 던진다")
    @Test
    void throwsCourseNotFoundWhenGhostLeaderboardCourseIsMissing() {
        long courseId = 999L;
        when(courseRepository.findNotDeletedCourse(courseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.findCourseGhostLeaderboard(courseId, new CourseGhostLeaderboardRequest(20, null)))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.COURSE_NOT_FOUND);
    }

    @DisplayName("코스 리더보드는 point 내림차순 rank와 페이지 정보를 반환한다")
    @Test
    void returnsCourseLeaderboardWithRankingAndPagination() {
        long courseId = 12L;
        Course course = course(courseId);
        CourseLeaderboardRequest request = new CourseLeaderboardRequest(courseId, 1, 2);
        List<CourseGhostLeaderboardRankRow> rows = List.of(
                new CourseGhostLeaderboardRankRow(11L, 101L, "runner-b", 301L, 200, 2L, LocalDateTime.of(2026, 3, 15, 10, 0)),
                new CourseGhostLeaderboardRankRow(12L, 102L, "runner-c", 302L, 100, 3L, LocalDateTime.of(2026, 3, 15, 10, 5))
        );

        when(courseRepository.findNotDeletedCourse(courseId)).thenReturn(Optional.of(course));
        when(courseGhostLeaderboardRepository.findRankedRowsByCourseId(courseId, 1, 2)).thenReturn(rows);
        when(courseGhostLeaderboardRepository.countByCourseId(courseId)).thenReturn(5L);

        CourseLeaderboardResponse response = courseService.findCourseLeaderboard(request);

        assertThat(response.courseId()).isEqualTo(courseId);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(5L);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().getFirst().rank()).isEqualTo(2L);
        assertThat(response.items().getFirst().nickname()).isEqualTo("runner-b");
    }

    @DisplayName("없는 코스의 리더보드 조회는 코스 없음 예외를 던진다")
    @Test
    void throwsCourseNotFoundWhenLeaderboardCourseIsMissing() {
        long courseId = 999L;
        CourseLeaderboardRequest request = new CourseLeaderboardRequest(courseId, 0, 10);
        when(courseRepository.findNotDeletedCourse(courseId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> courseService.findCourseLeaderboard(request))
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

    private User user(Long id, String nickname) {
        User user = User.createPendingSocialUser();
        setField(user, "id", id);
        setField(user, "nickname", nickname);
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
