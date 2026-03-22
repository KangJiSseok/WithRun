package kr.withrun.was.domain.course.service;

import kr.withrun.was.domain.course.dto.NearbyCourseItemResponse;
import kr.withrun.was.domain.course.dto.NearbyCoursesRequest;
import kr.withrun.was.domain.course.dto.NearbyCoursesResponse;
import kr.withrun.was.domain.course.dto.NearbyGhostCoursesRequest;
import kr.withrun.was.domain.course.dto.NearbyGhostCoursesResponse;
import kr.withrun.was.domain.course.dto.PreferredDistanceRange;
import kr.withrun.was.domain.course.repository.CourseBookmarkRepository;
import kr.withrun.was.domain.course.repository.CourseDifficultyRepository;
import kr.withrun.was.domain.course.repository.CourseGhostLeaderboardRepository;
import kr.withrun.was.domain.course.repository.CourseLikeRepository;
import kr.withrun.was.domain.course.repository.CourseRepository;
import kr.withrun.was.domain.course.repository.CourseReviewRepository;
import kr.withrun.was.domain.course.repository.CourseTypeMapRepository;
import kr.withrun.was.domain.course.repository.query.dto.NearbyCourseCandidateRow;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.domain.course.util.NearbyCourseCursorCodec;
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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Nearby course query service")
class NearbyCourseQueryServiceTest {

    private static final double USER_LATITUDE = 37.5665;
    private static final double USER_LONGITUDE = 126.9780;

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

        lenient().when(courseLikeRepository.countByCourseIds(anyList())).thenReturn(Map.of());
        lenient().when(courseBookmarkRepository.countByCourseIds(anyList())).thenReturn(Map.of());
    }

    @DisplayName("filters courses outside target distance window")
    @Test
    void filtersCoursesOutsideTargetDistanceWindow() {
        NearbyCoursesRequest request = nearbyCoursesRequest(5000, null, null);
        stubCandidates(
                candidate(2L, 9000, coordinatesAtUser(), CourseType.RIVERSIDE),
                candidate(1L, 11000, coordinatesAtUser(), CourseType.PARK),
                candidate(3L, 8999, coordinatesAtUser(), CourseType.OTHER),
                candidate(4L, 11001, coordinatesAtUser(), CourseType.TRACK)
        );

        List<NearbyCourseItemResponse> items = courseService.findNearbyCourseItems(request);

        assertThat(items)
                .extracting(NearbyCourseItemResponse::courseId)
                .containsExactly(1L, 2L);
    }

    @DisplayName("filters courses outside requested radius")
    @Test
    void filtersCoursesOutsideRequestedRadius() {
        double targetLatitude = USER_LATITUDE + (300 / 111_139d);
        NearbyCoursesRequest request = nearbyCoursesRequest(targetLatitude, USER_LONGITUDE, 1000, null, null);
        stubCandidates(
                candidate(1L, 10000, coordinatesNorthOfUser(300), CourseType.RIVERSIDE),
                candidate(2L, 10000, coordinatesNorthOfUser(1400), CourseType.PARK)
        );

        List<NearbyCourseItemResponse> items = courseService.findNearbyCourseItems(request);

        assertThat(items)
                .extracting(NearbyCourseItemResponse::courseId)
                .containsExactly(1L);
        assertThat(items.get(0).distanceFromUserM()).isEqualTo(300);
    }

    @DisplayName("calculates minimum distance using all coordinate pairs")
    @Test
    void calculatesMinimumDistanceUsingAllCoordinatePairs() {
        NearbyCoursesRequest request = nearbyCoursesRequest(1000, null, null);
        stubCandidates(
                candidate(
                        1L,
                        10000,
                        coordinates(List.of(35.1796, USER_LATITUDE), List.of(129.0756, USER_LONGITUDE)),
                        CourseType.URBAN,
                        35.1796,
                        129.0756,
                        Difficulty.HARD
                )
        );

        List<NearbyCourseItemResponse> items = courseService.findNearbyCourseItems(request);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).distanceFromUserM()).isZero();
        assertThat(items.get(0).courseId()).isEqualTo(1L);
    }

    @DisplayName("excludes malformed or empty coordinates")
    @Test
    void excludesMalformedOrEmptyCoordinates() {
        NearbyCoursesRequest request = nearbyCoursesRequest(5000, null, null);
        stubCandidates(
                candidate(1L, 10000, coordinatesAtUser(), CourseType.RIVERSIDE),
                candidate(2L, 10000, null, CourseType.PARK),
                candidate(3L, 10000, coordinates(List.of(), List.of()), CourseType.TRACK),
                candidate(4L, 10000, coordinates(List.of(USER_LATITUDE), List.of()), CourseType.OTHER),
                candidate(5L, 10000, coordinates(Arrays.asList((Double) null), Arrays.asList((Double) null)), CourseType.URBAN)
        );

        List<NearbyCourseItemResponse> items = courseService.findNearbyCourseItems(request);

        assertThat(items)
                .extracting(NearbyCourseItemResponse::courseId)
                .containsExactly(1L);
    }

    @DisplayName("sorts same distance by course id")
    @Test
    void sortsSameDistanceByCourseId() {
        NearbyCoursesRequest request = nearbyCoursesRequest(5000, null, null);
        stubCandidates(
                candidate(20L, 10000, coordinatesAtUser(), CourseType.RIVERSIDE),
                candidate(10L, 10000, coordinatesAtUser(), CourseType.PARK)
        );

        List<NearbyCourseItemResponse> items = courseService.findNearbyCourseItems(request);

        assertThat(items)
                .extracting(NearbyCourseItemResponse::courseId)
                .containsExactly(10L, 20L);
    }

    @DisplayName("aggregates course types across repository rows per course")
    @Test
    void aggregatesCourseTypesAcrossRepositoryRowsPerCourse() {
        NearbyCoursesRequest request = nearbyCoursesRequest(5000, null, null);
        stubCandidates(
                candidate(7L, 10000, coordinatesAtUser(), CourseType.PARK),
                candidate(7L, 10000, coordinatesAtUser(), CourseType.RIVERSIDE),
                candidate(7L, 10000, coordinatesAtUser(), CourseType.PARK)
        );

        List<NearbyCourseItemResponse> items = courseService.findNearbyCourseItems(request);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).courseId()).isEqualTo(7L);
        assertThat(items.get(0).courseTypes())
                .containsExactly(
                        new NearbyCourseItemResponse.CourseTypeOption("RIVERSIDE", "강변"),
                        new NearbyCourseItemResponse.CourseTypeOption("PARK", "공원")
                );
    }

    @DisplayName("returns first three items and next cursor when page size is missing")
    @Test
    void returnsFirstThreeItemsAndNextCursorWhenPageSizeIsMissing() {
        NearbyCoursesRequest request = nearbyCoursesRequest(1000, null, null);
        stubCandidates(
                candidate(15L, 10000, coordinatesAtUser(), CourseType.RIVERSIDE),
                candidate(12L, 10000, coordinatesAtUser(), CourseType.RIVERSIDE),
                candidate(14L, 10000, coordinatesAtUser(), CourseType.RIVERSIDE),
                candidate(11L, 10000, coordinatesAtUser(), CourseType.RIVERSIDE),
                candidate(10L, 10000, coordinatesAtUser(), CourseType.RIVERSIDE),
                candidate(13L, 10000, coordinatesAtUser(), CourseType.RIVERSIDE)
        );

        NearbyCoursesResponse response = findNearbyCourses(request);

        assertThat(response.items())
                .extracting(NearbyCourseItemResponse::courseId)
                .containsExactly(10L, 11L, 12L);
        assertThat(response.hasMore()).isTrue();
        assertThat(response.nextCursor()).isEqualTo(NearbyCourseCursorCodec.encode(0, 12L));
        assertThat(response.suggestedNextRadiusM()).isNull();
    }

    @DisplayName("returns requested item count when page size is provided")
    @Test
    void returnsRequestedItemCountWhenPageSizeIsProvided() {
        NearbyCoursesRequest request = nearbyCoursesRequest(1000, 10, null);
        stubCandidates(
                candidate(15L, 10000, coordinatesAtUser(), CourseType.RIVERSIDE),
                candidate(12L, 10000, coordinatesAtUser(), CourseType.RIVERSIDE),
                candidate(14L, 10000, coordinatesAtUser(), CourseType.RIVERSIDE),
                candidate(11L, 10000, coordinatesAtUser(), CourseType.RIVERSIDE),
                candidate(10L, 10000, coordinatesAtUser(), CourseType.RIVERSIDE),
                candidate(13L, 10000, coordinatesAtUser(), CourseType.RIVERSIDE),
                candidate(16L, 10000, coordinatesAtUser(), CourseType.RIVERSIDE),
                candidate(17L, 10000, coordinatesAtUser(), CourseType.RIVERSIDE),
                candidate(18L, 10000, coordinatesAtUser(), CourseType.RIVERSIDE),
                candidate(19L, 10000, coordinatesAtUser(), CourseType.RIVERSIDE),
                candidate(20L, 10000, coordinatesAtUser(), CourseType.RIVERSIDE)
        );

        NearbyCoursesResponse response = findNearbyCourses(request);

        assertThat(response.items())
                .extracting(NearbyCourseItemResponse::courseId)
                .containsExactly(10L, 11L, 12L, 13L, 14L, 15L, 16L, 17L, 18L, 19L);
        assertThat(response.hasMore()).isTrue();
        assertThat(response.nextCursor()).isEqualTo(NearbyCourseCursorCodec.encode(0, 19L));
        assertThat(response.suggestedNextRadiusM()).isNull();
    }

    @DisplayName("enriches paged nearby courses with batched like and bookmark counts")
    @Test
    void enrichesPagedNearbyCoursesWithBatchedLikeAndBookmarkCounts() {
        NearbyCoursesRequest request = nearbyCoursesRequest(1000, 2, null);
        stubCandidates(
                candidate(10L, 10000, coordinatesAtUser(), CourseType.RIVERSIDE),
                candidate(11L, 10000, coordinatesNorthOfUser(300), CourseType.PARK),
                candidate(12L, 10000, coordinatesNorthOfUser(600), CourseType.TRACK)
        );
        when(courseLikeRepository.countByCourseIds(List.of(10L, 11L)))
                .thenReturn(Map.of(10L, 7L));
        when(courseBookmarkRepository.countByCourseIds(List.of(10L, 11L)))
                .thenReturn(Map.of(11L, 3L));

        NearbyCoursesResponse response = findNearbyCourses(request);

        assertThat(response.items())
                .extracting(
                        NearbyCourseItemResponse::courseId,
                        NearbyCourseItemResponse::likeCount,
                        NearbyCourseItemResponse::bookmarkCount
                )
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(10L, 7L, 0L),
                        org.assertj.core.groups.Tuple.tuple(11L, 0L, 3L)
                );
    }

    @DisplayName("resumes strictly after cursor tuple")
    @Test
    void resumesStrictlyAfterCursorTuple() {
        NearbyCoursesRequest baseRequest = nearbyCoursesRequest(1000, null, null);
        stubCandidates(
                candidate(1L, 10000, coordinatesAtUser(), CourseType.RIVERSIDE),
                candidate(2L, 10000, coordinatesAtUser(), CourseType.PARK),
                candidate(3L, 10000, coordinatesNorthOfUser(300), CourseType.TRACK),
                candidate(4L, 10000, coordinatesNorthOfUser(300), CourseType.OTHER),
                candidate(5L, 10000, coordinatesNorthOfUser(600), CourseType.URBAN)
        );

        List<NearbyCourseItemResponse> sortedItems = courseService.findNearbyCourseItems(baseRequest);
        NearbyCourseItemResponse cursorItem = sortedItems.get(2);
        NearbyCoursesRequest pagedRequest = nearbyCoursesRequest(
                1000,
                null,
                NearbyCourseCursorCodec.encode(cursorItem.distanceFromUserM(), cursorItem.courseId())
        );

        NearbyCoursesResponse response = findNearbyCourses(pagedRequest);

        assertThat(response.items())
                .extracting(NearbyCourseItemResponse::courseId)
                .containsExactly(sortedItems.get(3).courseId(), sortedItems.get(4).courseId());
        assertThat(response.hasMore()).isFalse();
        assertThat(response.nextCursor()).isNull();
    }

    @DisplayName("throws invalid cursor when cursor cannot be decoded")
    @Test
    void throwsInvalidCursorWhenCursorCannotBeDecoded() {
        NearbyCoursesRequest request = nearbyCoursesRequest(1000, null, "%%%");
        stubCandidates();

        assertThatThrownBy(() -> findNearbyCourses(request))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.INVALID_CURSOR);
    }

    @DisplayName("suggests next radius when current radius is exhausted before max radius")
    @Test
    void suggestsNextRadiusWhenCurrentRadiusIsExhaustedBeforeMaxRadius() {
        NearbyCoursesRequest request = nearbyCoursesRequest(1000, null, null);
        stubCandidates(
                candidate(1L, 10000, coordinatesAtUser(), CourseType.RIVERSIDE),
                candidate(2L, 10000, coordinatesNorthOfUser(900), CourseType.PARK)
        );

        NearbyCoursesResponse response = findNearbyCourses(request);

        assertThat(response.items())
                .extracting(NearbyCourseItemResponse::courseId)
                .containsExactly(1L, 2L);
        assertThat(response.hasMore()).isFalse();
        assertThat(response.nextCursor()).isNull();
        assertThat(response.suggestedNextRadiusM()).isEqualTo(2000);
    }

    @DisplayName("suggests next radius from arbitrary radius input")
    @Test
    void suggestsNextRadiusFromArbitraryRadiusInput() {
        NearbyCoursesRequest request = nearbyCoursesRequest(1500, null, null);
        stubCandidates(
                candidate(1L, 10000, coordinatesAtUser(), CourseType.RIVERSIDE),
                candidate(2L, 10000, coordinatesNorthOfUser(1400), CourseType.PARK)
        );

        NearbyCoursesResponse response = findNearbyCourses(request);

        assertThat(response.items())
                .extracting(NearbyCourseItemResponse::courseId)
                .containsExactly(1L, 2L);
        assertThat(response.hasMore()).isFalse();
        assertThat(response.nextCursor()).isNull();
        assertThat(response.suggestedNextRadiusM()).isEqualTo(2500);
    }

    @DisplayName("caps suggested next radius at maximum radius")
    @Test
    void capsSuggestedNextRadiusAtMaximumRadius() {
        NearbyCoursesRequest request = nearbyCoursesRequest(4500, null, null);
        stubCandidates(
                candidate(1L, 10000, coordinatesAtUser(), CourseType.RIVERSIDE),
                candidate(2L, 10000, coordinatesNorthOfUser(4400), CourseType.PARK)
        );

        NearbyCoursesResponse response = findNearbyCourses(request);

        assertThat(response.hasMore()).isFalse();
        assertThat(response.nextCursor()).isNull();
        assertThat(response.suggestedNextRadiusM()).isEqualTo(5000);
    }

    @DisplayName("keeps suggested next radius null at maximum radius")
    @Test
    void keepsSuggestedNextRadiusNullAtMaximumRadius() {
        NearbyCoursesRequest request = nearbyCoursesRequest(5000, null, null);
        stubCandidates(
                candidate(1L, 10000, coordinatesAtUser(), CourseType.RIVERSIDE),
                candidate(2L, 10000, coordinatesNorthOfUser(900), CourseType.PARK)
        );

        NearbyCoursesResponse response = findNearbyCourses(request);

        assertThat(response.hasMore()).isFalse();
        assertThat(response.nextCursor()).isNull();
        assertThat(response.suggestedNextRadiusM()).isNull();
    }

    @DisplayName("sorts nearby ghost courses by distance then course id")
    @Test
    void sortsNearbyGhostCoursesByDistanceThenCourseId() {
        NearbyGhostCoursesRequest request = new NearbyGhostCoursesRequest(
                USER_LATITUDE,
                USER_LONGITUDE,
                List.of(range(4800, 5200)),
                null,
                null
        );
        stubGhostCandidates(
                request.preferredDistanceMs(),
                candidate(20L, 5000, coordinatesAtUser(), CourseType.RIVERSIDE),
                candidate(10L, 5000, coordinatesAtUser(), CourseType.PARK),
                candidate(30L, 5000, coordinatesNorthOfUser(400), CourseType.TRACK)
        );

        List<NearbyCourseItemResponse> items = courseService.findNearbyGhostCourseItems(request);

        assertThat(items)
                .extracting(NearbyCourseItemResponse::courseId)
                .containsExactly(10L, 20L, 30L);
    }

    @DisplayName("aggregates course types across repository rows per ghost course")
    @Test
    void aggregatesCourseTypesAcrossRepositoryRowsPerGhostCourse() {
        NearbyGhostCoursesRequest request = new NearbyGhostCoursesRequest(
                USER_LATITUDE,
                USER_LONGITUDE,
                List.of(range(4800, 5200)),
                null,
                null
        );
        stubGhostCandidates(
                request.preferredDistanceMs(),
                candidate(7L, 5000, coordinatesAtUser(), CourseType.PARK),
                candidate(7L, 5000, coordinatesAtUser(), CourseType.RIVERSIDE),
                candidate(7L, 5000, coordinatesAtUser(), CourseType.PARK)
        );

        List<NearbyCourseItemResponse> items = courseService.findNearbyGhostCourseItems(request);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).courseId()).isEqualTo(7L);
        assertThat(items.get(0).courseTypes())
                .extracting(NearbyCourseItemResponse.CourseTypeOption::data)
                .containsExactly("RIVERSIDE", "PARK");
    }

    @DisplayName("returns nearby ghost courses with cursor pagination")
    @Test
    void returnsNearbyGhostCoursesWithCursorPagination() {
        NearbyGhostCoursesRequest request = new NearbyGhostCoursesRequest(
                USER_LATITUDE,
                USER_LONGITUDE,
                List.of(range(4800, 5200)),
                2,
                null
        );
        stubGhostCandidates(
                request.preferredDistanceMs(),
                candidate(15L, 5000, coordinatesNorthOfUser(600), CourseType.RIVERSIDE),
                candidate(12L, 5000, coordinatesNorthOfUser(300), CourseType.RIVERSIDE),
                candidate(10L, 5000, coordinatesAtUser(), CourseType.RIVERSIDE),
                candidate(11L, 5000, coordinatesNorthOfUser(300), CourseType.RIVERSIDE)
        );

        NearbyGhostCoursesResponse response = courseService.findNearbyGhostCourses(request);

        assertThat(response.items())
                .extracting(NearbyCourseItemResponse::courseId)
                .containsExactly(10L, 11L);
        assertThat(response.hasMore()).isTrue();
        assertThat(response.nextCursor()).isEqualTo(NearbyCourseCursorCodec.encode(300, 11L));
    }

    @DisplayName("resumes nearby ghost courses strictly after cursor tuple")
    @Test
    void resumesNearbyGhostCoursesStrictlyAfterCursorTuple() {
        NearbyGhostCoursesRequest baseRequest = new NearbyGhostCoursesRequest(
                USER_LATITUDE,
                USER_LONGITUDE,
                List.of(range(4800, 5200)),
                null,
                null
        );
        stubGhostCandidates(
                baseRequest.preferredDistanceMs(),
                candidate(1L, 5000, coordinatesAtUser(), CourseType.RIVERSIDE),
                candidate(2L, 5000, coordinatesNorthOfUser(300), CourseType.PARK),
                candidate(3L, 5000, coordinatesNorthOfUser(300), CourseType.TRACK),
                candidate(4L, 5000, coordinatesNorthOfUser(600), CourseType.OTHER)
        );

        List<NearbyCourseItemResponse> sortedItems = courseService.findNearbyGhostCourseItems(baseRequest);
        NearbyCourseItemResponse cursorItem = sortedItems.get(1);
        NearbyGhostCoursesRequest pagedRequest = new NearbyGhostCoursesRequest(
                USER_LATITUDE,
                USER_LONGITUDE,
                List.of(range(4800, 5200)),
                10,
                NearbyCourseCursorCodec.encode(cursorItem.distanceFromUserM(), cursorItem.courseId())
        );

        NearbyGhostCoursesResponse response = courseService.findNearbyGhostCourses(pagedRequest);

        assertThat(response.items())
                .extracting(NearbyCourseItemResponse::courseId)
                .containsExactly(3L, 4L);
        assertThat(response.hasMore()).isFalse();
        assertThat(response.nextCursor()).isNull();
    }

    @DisplayName("throws invalid cursor when ghost cursor cannot be decoded")
    @Test
    void throwsInvalidCursorWhenGhostCursorCannotBeDecoded() {
        NearbyGhostCoursesRequest request = new NearbyGhostCoursesRequest(
                USER_LATITUDE,
                USER_LONGITUDE,
                List.of(range(4800, 5200)),
                null,
                "%%%"
        );
        stubGhostCandidates(request.preferredDistanceMs());

        assertThatThrownBy(() -> courseService.findNearbyGhostCourses(request))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.INVALID_CURSOR);
    }

    private void stubCandidates(NearbyCourseCandidateRow... candidates) {
        when(courseRepository.findOfficialNearbyCourseCandidates(anyList())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<PreferredDistanceRange> preferredDistanceMs = invocation.getArgument(0);
            return List.of(candidates).stream()
                    .filter(candidate -> preferredDistanceMs.stream()
                            .anyMatch(range -> candidate.distanceM() >= range.min() && candidate.distanceM() <= range.max()))
                    .toList();
        });
    }

    private void stubGhostCandidates(List<PreferredDistanceRange> preferredDistanceMs, NearbyCourseCandidateRow... candidates) {
        when(courseRepository.findOfficialNearbyCourseCandidates(preferredDistanceMs)).thenReturn(List.of(candidates));
    }

    private PreferredDistanceRange range(int min, int max) {
        return new PreferredDistanceRange(min, max);
    }

    private NearbyCoursesResponse findNearbyCourses(NearbyCoursesRequest request) {
        return courseService.findNearbyCourses(request);
    }

    private NearbyCoursesRequest nearbyCoursesRequest(int radiusM, Integer pageSize, String cursor) {
        return nearbyCoursesRequest(USER_LATITUDE, USER_LONGITUDE, radiusM, pageSize, cursor);
    }

    private NearbyCoursesRequest nearbyCoursesRequest(
            double targetLatitude,
            double targetLongitude,
            int radiusM,
            Integer pageSize,
            String cursor
    ) {
        return new NearbyCoursesRequest(
                USER_LATITUDE,
                USER_LONGITUDE,
                targetLatitude,
                targetLongitude,
                radiusM,
                List.of(range(9000, 11000)),
                pageSize,
                cursor
        );
    }

    private NearbyCourseCandidateRow candidate(
            Long courseId,
            Integer distanceM,
            Coordinates coordinates,
            CourseType courseType
    ) {
        return candidate(courseId, distanceM, coordinates, courseType, USER_LATITUDE, USER_LONGITUDE, Difficulty.MEDIUM);
    }

    private NearbyCourseCandidateRow candidate(
            Long courseId,
            Integer distanceM,
            Coordinates coordinates,
            CourseType courseType,
            Double startLatitude,
            Double startLongitude,
            Difficulty difficulty
    ) {
        return new NearbyCourseCandidateRow(
                courseId,
                "Course " + courseId,
                CourseStatus.OFFICIAL,
                distanceM,
                120,
                startLatitude,
                startLongitude,
                startLatitude + 0.01,
                startLongitude + 0.01,
                "snapshot-" + courseId,
                coordinates,
                difficulty,
                courseType
        );
    }

    private Coordinates coordinatesAtUser() {
        return coordinates(List.of(USER_LATITUDE), List.of(USER_LONGITUDE));
    }

    private Coordinates coordinatesNorthOfUser(int distanceM) {
        double latitudeOffsetDegrees = distanceM / 111_139d;
        return coordinates(List.of(USER_LATITUDE + latitudeOffsetDegrees), List.of(USER_LONGITUDE));
    }

    private Coordinates coordinates(List<Double> latitudes, List<Double> longitudes) {
        int elevationSize = latitudes != null ? latitudes.size() : (longitudes != null ? longitudes.size() : 0);
        return new Coordinates(latitudes, longitudes, java.util.Collections.nCopies(elevationSize, 0.0));
    }
}
