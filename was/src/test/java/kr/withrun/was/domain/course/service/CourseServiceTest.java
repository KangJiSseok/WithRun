package kr.withrun.was.domain.course.service;

import kr.withrun.was.domain.course.dto.CourseDetailResponse;
import kr.withrun.was.domain.course.dto.CourseGhostDetailResponse;
import kr.withrun.was.domain.course.dto.CourseGhostLeaderboardRequest;
import kr.withrun.was.domain.course.dto.CourseGhostLeaderboardResponse;
import kr.withrun.was.domain.course.dto.CourseRegisterMetaResponse;
import kr.withrun.was.domain.course.dto.CourseSurveyMetaResponse;
import kr.withrun.was.domain.course.dto.NearbyCoursesRequest;
import kr.withrun.was.domain.course.dto.NearbyCoursesResponse;
import kr.withrun.was.domain.course.dto.NearbyGhostCoursesRequest;
import kr.withrun.was.domain.course.dto.NearbyGhostCoursesResponse;
import kr.withrun.was.domain.course.dto.PreferredDistanceRange;
import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.entity.CourseGhostLeaderboard;
import kr.withrun.was.domain.course.repository.CourseBookmarkRepository;
import kr.withrun.was.domain.course.repository.CourseDifficultyRepository;
import kr.withrun.was.domain.course.repository.CourseGhostLeaderboardRepository;
import kr.withrun.was.domain.course.repository.CourseLikeRepository;
import kr.withrun.was.domain.course.repository.CourseRepository;
import kr.withrun.was.domain.course.repository.CourseReviewRepository;
import kr.withrun.was.domain.course.repository.CourseTypeMapRepository;
import kr.withrun.was.domain.course.repository.query.dto.CourseGhostLeaderboardRankRow;
import kr.withrun.was.domain.course.repository.query.dto.NearbyCoursePageRow;
import kr.withrun.was.domain.file.service.CloudFrontSignedUrlService;
import kr.withrun.was.domain.course.service.recommendation.HybridRecommendationService;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.domain.course.type.NearbyCourseSortBy;
import kr.withrun.was.domain.course.type.NearbyGhostCourseSortBy;
import kr.withrun.was.domain.course.type.RouteType;
import kr.withrun.was.domain.course.util.CourseGhostLeaderboardCursorCodec;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.domain.running.entity.RunningSession;
import kr.withrun.was.domain.running.type.RunningMode;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.repository.UserRepository;
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
import static org.mockito.Mockito.lenient;
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
    private CourseBookmarkRepository courseBookmarkRepository;

    @Mock
    private CourseLikeRepository courseLikeRepository;

    @Mock
    private CourseReviewRepository courseReviewRepository;

    @Mock
    private CourseGhostLeaderboardRepository courseGhostLeaderboardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private HybridRecommendationService hybridRecommendationService;

    @Mock
    private CloudFrontSignedUrlService cloudFrontSignedUrlService;

    private CourseService courseService;

    @BeforeEach
    void setUp() {
        courseService = new CourseService(
                courseRepository,
                courseDifficultyRepository,
                courseTypeMapRepository,
                courseBookmarkRepository,
                courseLikeRepository,
                courseReviewRepository,
                courseGhostLeaderboardRepository,
                userRepository,
                hybridRecommendationService,
                cloudFrontSignedUrlService
        );
        lenient().when(cloudFrontSignedUrlService.generateSignedUrl(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(invocation -> {
                    Object value = invocation.getArgument(0);
                    return value == null ? null : "signed::" + value;
                });
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
        assertThat(response.routeType()).isEqualTo(RouteType.LOOP);
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

    @DisplayName("코스 등록 메타 조회는 mode 를 포함한 enum 목록을 data와 label로 반환한다")
    @Test
    void returnsCourseRegisterMeta() {
        CourseRegisterMetaResponse response = courseService.findCourseRegisterMeta();

        assertThat(response.courseTypes()).containsExactly(
                new CourseRegisterMetaResponse.CourseTypeOption("RIVERSIDE", "강변"),
                new CourseRegisterMetaResponse.CourseTypeOption("PARK", "공원"),
                new CourseRegisterMetaResponse.CourseTypeOption("MOUNTAIN_TRAIL", "산악"),
                new CourseRegisterMetaResponse.CourseTypeOption("TRACK", "트랙"),
                new CourseRegisterMetaResponse.CourseTypeOption("URBAN", "도심"),
                new CourseRegisterMetaResponse.CourseTypeOption("OTHER", "기타")
        );
        assertThat(response.difficulties()).containsExactly(
                new CourseRegisterMetaResponse.DifficultyOption("EASY", "쉬움"),
                new CourseRegisterMetaResponse.DifficultyOption("MEDIUM", "보통"),
                new CourseRegisterMetaResponse.DifficultyOption("HARD", "어려움")
        );
        assertThat(response.mode()).containsExactly(
                new CourseRegisterMetaResponse.ModeOption("COMMUNITY", "커뮤니티 코스"),
                new CourseRegisterMetaResponse.ModeOption("PRIVATE", "개인 코스")
        );
        assertThat(response.routeTypes()).containsExactly(
                new CourseRegisterMetaResponse.RouteTypeOption("LOOP", "순환형"),
                new CourseRegisterMetaResponse.RouteTypeOption("OUT_AND_BACK", "왕복형")
        );
    }

    @DisplayName("설문 메타 조회는 mode 없이 enum 목록을 data와 label로 반환한다")
    @Test
    void returnsSurveyMeta() {
        CourseSurveyMetaResponse response = courseService.findSurveyMeta();

        assertThat(response.courseTypes()).containsExactly(
                new CourseSurveyMetaResponse.CourseTypeOption("RIVERSIDE", "강변"),
                new CourseSurveyMetaResponse.CourseTypeOption("PARK", "공원"),
                new CourseSurveyMetaResponse.CourseTypeOption("MOUNTAIN_TRAIL", "산악"),
                new CourseSurveyMetaResponse.CourseTypeOption("TRACK", "트랙"),
                new CourseSurveyMetaResponse.CourseTypeOption("URBAN", "도심"),
                new CourseSurveyMetaResponse.CourseTypeOption("OTHER", "기타")
        );
        assertThat(response.difficulties()).containsExactly(
                new CourseSurveyMetaResponse.DifficultyOption("EASY", "쉬움"),
                new CourseSurveyMetaResponse.DifficultyOption("MEDIUM", "보통"),
                new CourseSurveyMetaResponse.DifficultyOption("HARD", "어려움")
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
        assertThat(response.routeType()).isEqualTo(RouteType.LOOP);
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
        assertThat(response.routeType()).isEqualTo(RouteType.LOOP);
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
        assertThat(response.routeType()).isEqualTo(RouteType.LOOP);
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


    @Test
    void returnsNearbyCoursesWithOffsetPagination() {
        NearbyCoursesRequest request = new NearbyCoursesRequest(
                37.5665,
                126.9780,
                37.5700,
                126.9820,
                3000,
                List.of(new PreferredDistanceRange(1, 5000)),
                NearbyCourseSortBy.DISTANCE,
                1,
                2
        );
        List<NearbyCoursePageRow> rows = List.of(
                new NearbyCoursePageRow(
                        13L,
                        "Course 13",
                        CourseStatus.OFFICIAL,
                        RouteType.LOOP,
                        4300,
                        95,
                        37.5668,
                        126.9782,
                        37.5712,
                        126.9832,
                        "snapshot-13",
                        Difficulty.MEDIUM,
                        240,
                        170
                )
        );

        when(courseRepository.findNearbyCoursePageRows(
                request.preferredDistanceMs(),
                request.targetLatitude(),
                request.targetLongitude(),
                request.latitude(),
                request.longitude(),
                request.sortBy(),
                request.radiusM(),
                request.page(),
                request.size()
        )).thenReturn(rows);
        when(courseRepository.countNearbyCourses(
                request.preferredDistanceMs(),
                request.targetLatitude(),
                request.targetLongitude(),
                request.radiusM()
        )).thenReturn(3L);
        when(courseTypeMapRepository.findCourseTypesByCourseId(13L)).thenReturn(List.of(CourseType.TRACK));
        when(courseLikeRepository.countByCourseIds(List.of(13L))).thenReturn(java.util.Map.of(13L, 6L));
        when(courseBookmarkRepository.countByCourseIds(List.of(13L))).thenReturn(java.util.Map.of(13L, 1L));

        NearbyCoursesResponse response = courseService.findNearbyCourses(request, null);

        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(3L);
        assertThat(response.totalPages()).isEqualTo(2);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().courseId()).isEqualTo(13L);
        assertThat(response.items().getFirst().routeType()).isEqualTo(RouteType.LOOP);
        assertThat(response.items().getFirst().isRecommended()).isFalse();
        assertThat(response.items().getFirst().likeCount()).isEqualTo(6L);
        assertThat(response.items().getFirst().bookmarkCount()).isEqualTo(1L);
        assertThat(response.items().getFirst().snapshotImageUrl()).isEqualTo("signed::snapshot-13");
    }

    @Test
    void returnsNearbyGhostCoursesWithOffsetPagination() {
        NearbyGhostCoursesRequest request = new NearbyGhostCoursesRequest(
                37.5665,
                126.9780,
                37.5700,
                126.9820,
                3000,
                List.of(new PreferredDistanceRange(1, 5000)),
                NearbyGhostCourseSortBy.GHOST_RUN_COUNT,
                0,
                2
        );
        List<NearbyCoursePageRow> rows = List.of(
                new NearbyCoursePageRow(
                        21L,
                        "Ghost Course 21",
                        CourseStatus.OFFICIAL,
                        RouteType.OUT_AND_BACK,
                        5000,
                        120,
                        37.5665,
                        126.9780,
                        37.5700,
                        126.9820,
                        "snapshot-21",
                        Difficulty.EASY,
                        180,
                        140
                ),
                new NearbyCoursePageRow(
                        22L,
                        "Ghost Course 22",
                        CourseStatus.OFFICIAL,
                        RouteType.LOOP,
                        4200,
                        90,
                        37.5670,
                        126.9790,
                        37.5710,
                        126.9830,
                        "snapshot-22",
                        Difficulty.MEDIUM,
                        240,
                        170
                )
        );

        when(courseRepository.findNearbyGhostCoursePageRows(
                request.preferredDistanceMs(),
                request.targetLatitude(),
                request.targetLongitude(),
                request.latitude(),
                request.longitude(),
                request.sortBy(),
                request.radiusM(),
                request.page(),
                request.size()
        )).thenReturn(rows);
        when(courseRepository.countNearbyGhostCourses(
                request.preferredDistanceMs(),
                request.targetLatitude(),
                request.targetLongitude(),
                request.radiusM()
        )).thenReturn(2L);
        when(courseTypeMapRepository.findCourseTypesByCourseId(21L)).thenReturn(List.of(CourseType.RIVERSIDE));
        when(courseTypeMapRepository.findCourseTypesByCourseId(22L)).thenReturn(List.of(CourseType.PARK));
        when(courseLikeRepository.countByCourseIds(List.of(21L, 22L))).thenReturn(java.util.Map.of(21L, 5L, 22L, 7L));
        when(courseBookmarkRepository.countByCourseIds(List.of(21L, 22L))).thenReturn(java.util.Map.of(21L, 1L, 22L, 3L));

        NearbyGhostCoursesResponse response = courseService.findNearbyGhostCourses(request);

        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(2L);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().getFirst().courseId()).isEqualTo(21L);
        assertThat(response.items().getFirst().routeType()).isEqualTo(RouteType.OUT_AND_BACK);
        assertThat(response.items().getFirst().likeCount()).isEqualTo(5L);
        assertThat(response.items().getFirst().bookmarkCount()).isEqualTo(1L);
        assertThat(response.items().getFirst().snapshotImageUrl()).isEqualTo("signed::snapshot-21");
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
        setField(course, "routeType", RouteType.LOOP);
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
