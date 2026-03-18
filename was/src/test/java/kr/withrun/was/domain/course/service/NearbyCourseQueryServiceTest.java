package kr.withrun.was.domain.course.service;

import kr.withrun.was.domain.course.dto.NearbyCourseItemResponse;
import kr.withrun.was.domain.course.dto.NearbyCoursesRequest;
import kr.withrun.was.domain.course.dto.NearbyCoursesResponse;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("주변 코스 조회 서비스")
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

    @DisplayName("목표 거리 범위를 벗어난 코스는 제외한다")
    @Test
    void filtersCoursesOutsideTargetDistanceWindow() {
        NearbyCoursesRequest request = new NearbyCoursesRequest(USER_LATITUDE, USER_LONGITUDE, 5000, 10000, null, null);
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

    @DisplayName("요청 반경을 벗어난 코스는 제외한다")
    @Test
    void filtersCoursesOutsideRequestedRadius() {
        NearbyCoursesRequest request = new NearbyCoursesRequest(USER_LATITUDE, USER_LONGITUDE, 1000, 10000, null, null);
        stubCandidates(
                candidate(1L, 10000, coordinatesAtUser(), CourseType.RIVERSIDE),
                candidate(
                        2L,
                        10000,
                        coordinates(List.of(37.6165), List.of(USER_LONGITUDE)),
                        CourseType.PARK
                )
        );

        List<NearbyCourseItemResponse> items = courseService.findNearbyCourseItems(request);

        assertThat(items)
                .extracting(NearbyCourseItemResponse::courseId)
                .containsExactly(1L);
        assertThat(items.get(0).distanceFromUserM()).isZero();
    }

    @DisplayName("모든 좌표 쌍 중 최소 거리를 계산한다")
    @Test
    void calculatesMinimumDistanceUsingAllCoordinatePairs() {
        NearbyCoursesRequest request = new NearbyCoursesRequest(USER_LATITUDE, USER_LONGITUDE, 1000, 10000, null, null);
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

    @DisplayName("잘못되었거나 비어 있는 좌표는 제외한다")
    @Test
    void excludesMalformedOrEmptyCoordinates() {
        NearbyCoursesRequest request = new NearbyCoursesRequest(USER_LATITUDE, USER_LONGITUDE, 5000, 10000, null, null);
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

    @DisplayName("거리가 같은 코스는 코스 ID 순으로 정렬한다")
    @Test
    void sortsSameDistanceByCourseId() {
        NearbyCoursesRequest request = new NearbyCoursesRequest(USER_LATITUDE, USER_LONGITUDE, 5000, 10000, null, null);
        stubCandidates(
                candidate(20L, 10000, coordinatesAtUser(), CourseType.RIVERSIDE),
                candidate(10L, 10000, coordinatesAtUser(), CourseType.PARK)
        );

        List<NearbyCourseItemResponse> items = courseService.findNearbyCourseItems(request);

        assertThat(items)
                .extracting(NearbyCourseItemResponse::courseId)
                .containsExactly(10L, 20L);
    }

    @DisplayName("코스별로 리포지토리 행의 코스 타입을 합친다")
    @Test
    void aggregatesCourseTypesAcrossRepositoryRowsPerCourse() {
        NearbyCoursesRequest request = new NearbyCoursesRequest(USER_LATITUDE, USER_LONGITUDE, 5000, 10000, null, null);
        stubCandidates(
                candidate(7L, 10000, coordinatesAtUser(), CourseType.PARK),
                candidate(7L, 10000, coordinatesAtUser(), CourseType.RIVERSIDE),
                candidate(7L, 10000, coordinatesAtUser(), CourseType.PARK)
        );

        List<NearbyCourseItemResponse> items = courseService.findNearbyCourseItems(request);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).courseId()).isEqualTo(7L);
        assertThat(items.get(0).courseTypes())
                .containsExactly(CourseType.RIVERSIDE, CourseType.PARK);
    }

    @DisplayName("페이지 크기가 없으면 기본값 3개와 다음 커서를 반환한다")
    @Test
    void returnsFirstThreeItemsAndNextCursorWhenPageSizeIsMissing() {
        NearbyCoursesRequest request = new NearbyCoursesRequest(USER_LATITUDE, USER_LONGITUDE, 1000, 10000, null, null);
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

    @DisplayName("페이지 크기를 요청하면 해당 개수만큼 항목과 다음 커서를 반환한다")
    @Test
    void returnsRequestedItemCountWhenPageSizeIsProvided() {
        NearbyCoursesRequest request = new NearbyCoursesRequest(USER_LATITUDE, USER_LONGITUDE, 1000, 10000, 10, null);
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

    @DisplayName("커서 튜플 다음 항목부터 엄격하게 이어서 조회한다")
    @Test
    void resumesStrictlyAfterCursorTuple() {
        NearbyCoursesRequest baseRequest = new NearbyCoursesRequest(USER_LATITUDE, USER_LONGITUDE, 1000, 10000, null, null);
        stubCandidates(
                candidate(1L, 10000, coordinatesAtUser(), CourseType.RIVERSIDE),
                candidate(2L, 10000, coordinatesAtUser(), CourseType.PARK),
                candidate(3L, 10000, coordinatesNorthOfUser(300), CourseType.TRACK),
                candidate(4L, 10000, coordinatesNorthOfUser(300), CourseType.OTHER),
                candidate(5L, 10000, coordinatesNorthOfUser(600), CourseType.URBAN)
        );

        List<NearbyCourseItemResponse> sortedItems = courseService.findNearbyCourseItems(baseRequest);
        NearbyCourseItemResponse cursorItem = sortedItems.get(2);
        NearbyCoursesRequest pagedRequest = new NearbyCoursesRequest(
                USER_LATITUDE,
                USER_LONGITUDE,
                1000,
                10000,
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

    @DisplayName("커서를 해석할 수 없으면 잘못된 커서 예외를 던진다")
    @Test
    void throwsInvalidCursorWhenCursorCannotBeDecoded() {
        NearbyCoursesRequest request = new NearbyCoursesRequest(USER_LATITUDE, USER_LONGITUDE, 1000, 10000, null, "%%%");
        stubCandidates();

        assertThatThrownBy(() -> findNearbyCourses(request))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.INVALID_CURSOR);
    }

    @DisplayName("현재 반경이 최대 반경 전에 소진되면 다음 반경을 제안한다")
    @Test
    void suggestsNextRadiusWhenCurrentRadiusIsExhaustedBeforeMaxRadius() {
        NearbyCoursesRequest request = new NearbyCoursesRequest(USER_LATITUDE, USER_LONGITUDE, 1000, 10000, null, null);
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

    @DisplayName("임의의 반경 입력에서도 다음 반경을 제안한다")
    @Test
    void suggestsNextRadiusFromArbitraryRadiusInput() {
        NearbyCoursesRequest request = new NearbyCoursesRequest(USER_LATITUDE, USER_LONGITUDE, 1500, 10000, null, null);
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

    @DisplayName("제안하는 다음 반경은 최대 반경을 넘지 않는다")
    @Test
    void capsSuggestedNextRadiusAtMaximumRadius() {
        NearbyCoursesRequest request = new NearbyCoursesRequest(USER_LATITUDE, USER_LONGITUDE, 4500, 10000, null, null);
        stubCandidates(
                candidate(1L, 10000, coordinatesAtUser(), CourseType.RIVERSIDE),
                candidate(2L, 10000, coordinatesNorthOfUser(4400), CourseType.PARK)
        );

        NearbyCoursesResponse response = findNearbyCourses(request);

        assertThat(response.hasMore()).isFalse();
        assertThat(response.nextCursor()).isNull();
        assertThat(response.suggestedNextRadiusM()).isEqualTo(5000);
    }

    @DisplayName("최대 반경에서는 제안하는 다음 반경을 null로 유지한다")
    @Test
    void keepsSuggestedNextRadiusNullAtMaximumRadius() {
        NearbyCoursesRequest request = new NearbyCoursesRequest(USER_LATITUDE, USER_LONGITUDE, 5000, 10000, null, null);
        stubCandidates(
                candidate(1L, 10000, coordinatesAtUser(), CourseType.RIVERSIDE),
                candidate(2L, 10000, coordinatesNorthOfUser(900), CourseType.PARK)
        );

        NearbyCoursesResponse response = findNearbyCourses(request);

        assertThat(response.hasMore()).isFalse();
        assertThat(response.nextCursor()).isNull();
        assertThat(response.suggestedNextRadiusM()).isNull();
    }

    private void stubCandidates(NearbyCourseCandidateRow... candidates) {
        when(courseRepository.findOfficialNearbyCourseCandidates()).thenReturn(List.of(candidates));
    }

    private NearbyCoursesResponse findNearbyCourses(NearbyCoursesRequest request) {
        return courseService.findNearbyCourses(request);
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
