package kr.withrun.was.domain.course.service;

import kr.withrun.was.domain.course.dto.NearbyCoursesRequest;
import kr.withrun.was.domain.course.dto.NearbyCoursesResponse;
import kr.withrun.was.domain.course.dto.PreferredDistanceRange;
import kr.withrun.was.domain.course.repository.CourseBookmarkRepository;
import kr.withrun.was.domain.course.repository.CourseDifficultyRepository;
import kr.withrun.was.domain.course.repository.CourseGhostLeaderboardRepository;
import kr.withrun.was.domain.course.repository.CourseLikeRepository;
import kr.withrun.was.domain.course.repository.CourseRepository;
import kr.withrun.was.domain.course.repository.CourseReviewRepository;
import kr.withrun.was.domain.course.repository.CourseTypeMapRepository;
import kr.withrun.was.domain.course.repository.query.dto.NearbyCoursePageRow;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.domain.course.type.NearbyCourseSortBy;
import kr.withrun.was.global.common.type.Difficulty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("주변 코스 조회 서비스")
class NearbyCourseQueryServiceTest {

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
                courseGhostLeaderboardRepository
        );
        setField(courseService, "CDN_URL", "https://cdn.test");
    }

    @Test
    @DisplayName("페이지 row 조회 결과를 주변 코스 응답으로 변환한다")
    void returnsPagedNearbyCoursesFromRepositoryPageRows() {
        NearbyCoursesRequest request = request(3000, 0, 2);
        List<NearbyCoursePageRow> rows = List.of(
                pageRow(10L, 150, 100, Difficulty.EASY, "snapshot-10"),
                pageRow(20L, 220, 180, Difficulty.MEDIUM, "snapshot-20")
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
        )).thenReturn(5L);
        when(courseTypeMapRepository.findCourseTypesByCourseId(10L)).thenReturn(List.of(CourseType.RIVERSIDE));
        when(courseTypeMapRepository.findCourseTypesByCourseId(20L)).thenReturn(List.of(CourseType.PARK));
        when(courseLikeRepository.countByCourseIds(List.of(10L, 20L))).thenReturn(Map.of(10L, 3L, 20L, 7L));
        when(courseBookmarkRepository.countByCourseIds(List.of(10L, 20L))).thenReturn(Map.of(10L, 1L, 20L, 2L));

        NearbyCoursesResponse response = courseService.findNearbyCourses(request);

        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(5L);
        assertThat(response.totalPages()).isEqualTo(3);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.items()).hasSize(2);
        assertThat(response.items().getFirst().courseId()).isEqualTo(10L);
        assertThat(response.items().getFirst().likeCount()).isEqualTo(3L);
        assertThat(response.items().getFirst().bookmarkCount()).isEqualTo(1L);
        assertThat(response.items().getFirst().snapshotImageUrl()).isEqualTo("https://cdn.test/snapshot-10");
    }

    @Test
    @DisplayName("마지막 페이지에 도달하면 다음 탐색 반경을 제안한다")
    void suggestsLargerRadiusWhenLastPageIsReached() {
        NearbyCoursesRequest request = request(3000, 0, 3);
        List<NearbyCoursePageRow> rows = List.of(pageRow(10L, 150, 100, Difficulty.EASY, "snapshot-10"));

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
        )).thenReturn(1L);
        when(courseTypeMapRepository.findCourseTypesByCourseId(10L)).thenReturn(List.of(CourseType.RIVERSIDE));
        when(courseLikeRepository.countByCourseIds(List.of(10L))).thenReturn(Map.of());
        when(courseBookmarkRepository.countByCourseIds(List.of(10L))).thenReturn(Map.of());

        NearbyCoursesResponse response = courseService.findNearbyCourses(request);

        assertThat(response.hasNext()).isFalse();
    }

    @Test
    @DisplayName("인기순 정렬 요청도 정상 처리한다")
    void supportsPopularSortForNearbyCourses() {
        NearbyCoursesRequest request = request(3000, NearbyCourseSortBy.POPULAR, 0, 2);
        List<NearbyCoursePageRow> rows = List.of(pageRow(20L, 220, 180, Difficulty.MEDIUM, "snapshot-20"));

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
        )).thenReturn(1L);
        when(courseTypeMapRepository.findCourseTypesByCourseId(20L)).thenReturn(List.of(CourseType.PARK));
        when(courseLikeRepository.countByCourseIds(List.of(20L))).thenReturn(Map.of(20L, 7L));
        when(courseBookmarkRepository.countByCourseIds(List.of(20L))).thenReturn(Map.of(20L, 2L));

        NearbyCoursesResponse response = courseService.findNearbyCourses(request);

        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().courseId()).isEqualTo(20L);
        assertThat(response.items().getFirst().likeCount()).isEqualTo(7L);
        assertThat(response.items().getFirst().bookmarkCount()).isEqualTo(2L);
    }

    private NearbyCoursesRequest request(int radiusM, Integer page, Integer size) {
        return request(radiusM, NearbyCourseSortBy.DISTANCE, page, size);
    }

    private NearbyCoursesRequest request(int radiusM, NearbyCourseSortBy sortBy, Integer page, Integer size) {
        return new NearbyCoursesRequest(
                37.5665,
                126.9780,
                37.5700,
                126.9820,
                radiusM,
                List.of(new PreferredDistanceRange(1, 5000)),
                sortBy,
                page,
                size
        );
    }

    private NearbyCoursePageRow pageRow(
            Long courseId,
            int distanceFromTargetM,
            int distanceFromUserM,
            Difficulty difficulty,
            String snapshotImageUrl
    ) {
        return new NearbyCoursePageRow(
                courseId,
                "Course " + courseId,
                CourseStatus.OFFICIAL,
                5000,
                120,
                37.5665,
                126.9780,
                37.5700,
                126.9820,
                snapshotImageUrl,
                difficulty,
                distanceFromTargetM,
                distanceFromUserM
        );
    }

    private void setField(Object target, String fieldName, Object value) {
        Class<?> currentClass = target.getClass();
        while (currentClass != null) {
            try {
                java.lang.reflect.Field field = currentClass.getDeclaredField(fieldName);
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
