package kr.withrun.was.domain.course.service;

import kr.withrun.was.domain.course.dto.CourseDetailResponse;
import kr.withrun.was.domain.course.dto.CourseFilterResponse;
import kr.withrun.was.domain.course.dto.CourseGhostDetailResponse;
import kr.withrun.was.domain.course.dto.CourseGhostLeaderboardItemResponse;
import kr.withrun.was.domain.course.dto.CourseGhostLeaderboardRequest;
import kr.withrun.was.domain.course.dto.CourseGhostLeaderboardResponse;
import kr.withrun.was.domain.course.dto.CourseLeaderboardItemResponse;
import kr.withrun.was.domain.course.dto.CourseLeaderboardRequest;
import kr.withrun.was.domain.course.dto.CourseLeaderboardResponse;
import kr.withrun.was.domain.course.dto.CourseRegisterMetaResponse;
import kr.withrun.was.domain.course.dto.CourseSurveyMetaResponse;
import kr.withrun.was.domain.course.dto.NearbyCourseItemResponse;
import kr.withrun.was.domain.course.dto.NearbyCoursesRequest;
import kr.withrun.was.domain.course.dto.NearbyCoursesResponse;
import kr.withrun.was.domain.course.dto.NearbyGhostCoursesRequest;
import kr.withrun.was.domain.course.dto.NearbyGhostCoursesResponse;
import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.repository.CourseBookmarkRepository;
import kr.withrun.was.domain.course.repository.CourseDifficultyRepository;
import kr.withrun.was.domain.course.repository.CourseGhostLeaderboardRepository;
import kr.withrun.was.domain.course.repository.CourseLikeRepository;
import kr.withrun.was.domain.course.repository.CourseRepository;
import kr.withrun.was.domain.course.repository.CourseReviewRepository;
import kr.withrun.was.domain.course.repository.CourseTypeMapRepository;
import kr.withrun.was.domain.course.repository.query.dto.CourseGhostLeaderboardRankRow;
import kr.withrun.was.domain.course.repository.query.dto.NearbyCoursePageRow;
import kr.withrun.was.domain.course.type.CourseDistanceType;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.domain.course.util.CourseGhostLeaderboardCursorCodec;
import kr.withrun.was.global.common.type.Difficulty;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CourseService {

    @Value("${app.cdn.base-url}")
    private String CDN_URL;
    // 목표 거리 허용 오차는 +- 1km로 두고 비슷한 길이의 코스만 후보로 남긴다.
    // 요청 페이지 크기가 없으면 기본으로 3개를 내려준다.
    // 반경은 최대 5km까지만 점진적으로 확장한다.

    private final CourseRepository courseRepository;
    private final CourseDifficultyRepository courseDifficultyRepository;
    private final CourseTypeMapRepository courseTypeMapRepository;
    private final CourseBookmarkRepository courseBookmarkRepository;
    private final CourseLikeRepository courseLikeRepository;
    private final CourseReviewRepository courseReviewRepository;
    private final CourseGhostLeaderboardRepository courseGhostLeaderboardRepository;

    public NearbyCoursesResponse findNearbyCourses(NearbyCoursesRequest request) {
        List<NearbyCoursePageRow> pageRows = courseRepository.findNearbyCoursePageRows(
                request.preferredDistanceMs(),
                request.targetLatitude(),
                request.targetLongitude(),
                request.latitude(),
                request.longitude(),
                request.sortBy(),
                request.radiusM(),
                request.page(),
                request.size()
        );
        long totalElements = courseRepository.countNearbyCourses(
                request.preferredDistanceMs(),
                request.targetLatitude(),
                request.targetLongitude(),
                request.radiusM()
        );
        boolean hasNext = (long) (request.page() + 1) * request.size() < totalElements;
        int totalPages = totalElements == 0 ? 0 : (int) ((totalElements + request.size() - 1) / request.size());
        List<NearbyCourseItemResponse> items = enrichNearbyCourseItemsWithCounts(
                pageRows.stream()
                        .map(this::toNearbyCourseItemResponse)
                        .toList()
        );

        return new NearbyCoursesResponse(
                items,
                request.page(),
                request.size(),
                totalElements,
                totalPages,
                hasNext
        );
    }

    public NearbyGhostCoursesResponse findNearbyGhostCourses(NearbyGhostCoursesRequest request) {
        List<NearbyCoursePageRow> pageRows = courseRepository.findNearbyGhostCoursePageRows(
                request.preferredDistanceMs(),
                request.targetLatitude(),
                request.targetLongitude(),
                request.latitude(),
                request.longitude(),
                request.sortBy(),
                request.radiusM(),
                request.page(),
                request.size()
        );
        long totalElements = courseRepository.countNearbyGhostCourses(
                request.preferredDistanceMs(),
                request.targetLatitude(),
                request.targetLongitude(),
                request.radiusM()
        );
        boolean hasNext = (long) (request.page() + 1) * request.size() < totalElements;
        int totalPages = totalElements == 0 ? 0 : (int) ((totalElements + request.size() - 1) / request.size());
        List<NearbyCourseItemResponse> items = enrichNearbyCourseItemsWithCounts(
                pageRows.stream()
                        .map(this::toNearbyCourseItemResponse)
                        .toList()
        );

        return new NearbyGhostCoursesResponse(
                items,
                request.page(),
                request.size(),
                totalElements,
                totalPages,
                hasNext
        );
    }

    public CourseRegisterMetaResponse findCourseRegisterMeta() {
        return new CourseRegisterMetaResponse(
                Arrays.stream(CourseType.values())
                        .map(CourseRegisterMetaResponse.CourseTypeOption::from)
                        .toList(),
                List.of(CourseStatus.COMMUNITY, CourseStatus.PRIVATE).stream()
                        .map(CourseRegisterMetaResponse.ModeOption::from)
                        .toList(),
                Arrays.stream(Difficulty.values())
                        .map(CourseRegisterMetaResponse.DifficultyOption::from)
                        .toList()
        );
    }

    public CourseSurveyMetaResponse findSurveyMeta() {
        return new CourseSurveyMetaResponse(
                Arrays.stream(CourseType.values())
                        .map(CourseSurveyMetaResponse.CourseTypeOption::from)
                        .toList(),
                Arrays.stream(Difficulty.values())
                        .map(CourseSurveyMetaResponse.DifficultyOption::from)
                        .toList()
        );
    }

    public CourseFilterResponse findCourseFilters() {
        return new CourseFilterResponse(
                Arrays.stream(CourseDistanceType.values())
                        .map(CourseFilterResponse.CourseDistanceTypeOption::from)
                        .toList(),
                Arrays.stream(CourseType.values())
                        .map(CourseFilterResponse.CourseTypeOption::from)
                        .toList(),
                Arrays.stream(Difficulty.values())
                        .map(CourseFilterResponse.DifficultyOption::from)
                        .toList()
        );
    }

    public CourseDetailResponse findCourseDetail(Long courseId) {
        Course course = getCourse(courseId);

        return new CourseDetailResponse(
                course.getId(),
                course.getTitle(),
                course.getStatus(),
                CourseDetailResponse.DifficultyOption.from(courseDifficultyRepository.findDifficultyByCourseId(courseId)
                        .orElse(null)),
                course.getDistanceM(),
                course.getElevationGainM(),
                CDN_URL + "/" + course.getSnapshotImageUrl(),
                course.getStartLatitude(),
                course.getStartLongitude(),
                course.getEndLatitude(),
                course.getEndLongitude(),
                course.getCoordinates(),
                courseTypeMapRepository.findCourseTypesByCourseId(courseId).stream()
                        .map(CourseDetailResponse.CourseTypeOption::from)
                        .toList(),
                courseLikeRepository.countByCourseId(courseId),
                // TODO: 인증에서 userId를 전달받는 방식이 정해지면 isLiked/isBookmarked를 사용자 기준으로 계산하도록 수정해야 한다.
                false,
                false,
                courseReviewRepository.findAverageRatingByCourseId(courseId)
        );
    }

    public CourseGhostDetailResponse findCourseGhostDetail(Long courseId, Long userId) {
        Course course = getCourse(courseId);

        return CourseGhostDetailResponse.of(
                course,
                courseDifficultyRepository.findDifficultyByCourseId(courseId)
                        .orElse(null),
                courseTypeMapRepository.findCourseTypesByCourseId(courseId),
                courseLikeRepository.countByCourseId(courseId),
                false,
                false,
                courseReviewRepository.findAverageRatingByCourseId(courseId),
                findMyGhostRecord(courseId, userId)
        );
    }

    public CourseLeaderboardResponse findCourseLeaderboard(CourseLeaderboardRequest request) {
        getCourse(request.courseId());

        List<CourseGhostLeaderboardRankRow> rows = courseGhostLeaderboardRepository.findRankedRowsByCourseId(
                request.courseId(),
                request.page(),
                request.size()
        );
        long totalElements = courseGhostLeaderboardRepository.countByCourseId(request.courseId());

        List<CourseLeaderboardItemResponse> items = rows.stream()
                .map(row -> new CourseLeaderboardItemResponse(
                        row.leaderboardId(),
                        row.userId(),
                        row.nickname(),
                        row.runningSessionId(),
                        row.point(),
                        row.rank(),
                        row.createdAt()
                ))
                .toList();
        int totalPages = totalElements == 0 ? 0 : (int) ((totalElements + request.size() - 1) / request.size());
        boolean hasNext = (long) (request.page() + 1) * request.size() < totalElements;

        return new CourseLeaderboardResponse(
                request.courseId(),
                items,
                request.page(),
                request.size(),
                totalElements,
                totalPages,
                hasNext
        );
    }

    public CourseGhostLeaderboardResponse findCourseGhostLeaderboard(Long courseId, CourseGhostLeaderboardRequest request) {
        getCourse(courseId);

        List<CourseGhostLeaderboardRankRow> rows = courseGhostLeaderboardRepository.findRankedRowsByCourseId(courseId);
        int startIndex = resolveGhostLeaderboardStartIndex(rows, request.cursor());
        List<CourseGhostLeaderboardRankRow> remainingRows = rows.subList(startIndex, rows.size());
        boolean hasMore = remainingRows.size() > request.size();
        List<CourseGhostLeaderboardRankRow> pageRows = List.copyOf(remainingRows.subList(0, Math.min(request.size(), remainingRows.size())));
        String nextCursor = hasMore ? toGhostLeaderboardCursor(pageRows.getLast()) : null;

        List<CourseGhostLeaderboardItemResponse> items = pageRows.stream()
                .map(row -> new CourseGhostLeaderboardItemResponse(
                        row.leaderboardId(),
                        row.userId(),
                        row.nickname(),
                        row.runningSessionId(),
                        row.point(),
                        row.rank(),
                        row.createdAt()
                ))
                .toList();

        return new CourseGhostLeaderboardResponse(items, hasMore, nextCursor);
    }

    private Course getCourse(Long courseId) {
        Course course = courseRepository.findNotDeletedCourse(courseId)
                .orElseThrow(() -> new CustomException(ResponseCode.COURSE_NOT_FOUND));
        return course;
    }

    private CourseGhostDetailResponse.MyRecord findMyGhostRecord(Long courseId, Long userId) {
        return courseGhostLeaderboardRepository.findTopByUserIdAndCourseId(userId, courseId)
                .map(leaderboard -> new CourseGhostDetailResponse.MyRecord(
                        leaderboard.getId(),
                        leaderboard.getRunningSession().getId(),
                        leaderboard.getRunningSession().getDurationSec(),
                        leaderboard.getPoint(),
                        findRank(courseId, leaderboard.getId()),
                        leaderboard.getCreatedAt()
                ))
                .orElse(null);
    }

    private Long findRank(Long courseId, Long leaderboardId) {
        return courseGhostLeaderboardRepository.findRankedRowsByCourseId(courseId).stream()
                .filter(row -> row.leaderboardId().equals(leaderboardId))
                .map(CourseGhostLeaderboardRankRow::rank)
                .findFirst()
                .orElse(null);
    }

    private int resolveGhostLeaderboardStartIndex(List<CourseGhostLeaderboardRankRow> rows, String cursor) {
        if (cursor == null) {
            return 0;
        }

        CourseGhostLeaderboardCursorCodec.CursorPayload payload = CourseGhostLeaderboardCursorCodec.decode(cursor);
        for (int index = 0; index < rows.size(); index++) {
            if (isStrictlyAfter(rows.get(index), payload)) {
                return index;
            }
        }

        return rows.size();
    }

    private boolean isStrictlyAfter(
            CourseGhostLeaderboardRankRow row,
            CourseGhostLeaderboardCursorCodec.CursorPayload payload
    ) {
        return row.point() < payload.point()
                || (row.point().equals(payload.point()) && row.leaderboardId() > payload.leaderboardId());
    }

    private String toGhostLeaderboardCursor(CourseGhostLeaderboardRankRow row) {
        return CourseGhostLeaderboardCursorCodec.encode(row.point(), row.leaderboardId());
    }


    // 정렬 기준이 사용자와의 거리 -> courseId 이므로 같은 기준으로 "다음 데이터" 여부를 판단한다.
    private NearbyCourseItemResponse toNearbyCourseItemResponse(NearbyCoursePageRow row) {
        return new NearbyCourseItemResponse(
                row.courseId(),
                row.title(),
                row.status(),
                row.distanceM(),
                row.elevationGainM(),
                row.startLatitude(),
                row.startLongitude(),
                row.endLatitude(),
                row.endLongitude(),
                row.distanceFromTargetM(),
                row.distanceFromUserM(),
                NearbyCourseItemResponse.DifficultyOption.from(row.difficulty()),
                courseTypeMapRepository.findCourseTypesByCourseId(row.courseId()).stream()
                        .map(NearbyCourseItemResponse.CourseTypeOption::from)
                        .toList(),
                0L,
                0L,
                CDN_URL + "/" + row.snapshotImageUrl()
        );
    }

    private List<NearbyCourseItemResponse> enrichNearbyCourseItemsWithCounts(List<NearbyCourseItemResponse> items) {
        if (items.isEmpty()) return items;

        List<Long> courseIds = items.stream()
                .map(NearbyCourseItemResponse::courseId)
                .toList();

        Map<Long, Long> likeCountsByCourseIds = courseLikeRepository.countByCourseIds(courseIds);
        Map<Long, Long> bookmarkCountsByCourseIds = courseBookmarkRepository.countByCourseIds(courseIds);

        return items.stream()
                .map(item -> new NearbyCourseItemResponse(
                        item.courseId(),
                        item.title(),
                        item.status(),
                        item.distanceM(),
                        item.elevationGainM(),
                        item.startLatitude(),
                        item.startLongitude(),
                        item.endLatitude(),
                        item.endLongitude(),
                        item.distanceFromTargetM(),
                        item.distanceFromUserM(),
                        item.difficulty(),
                        item.courseTypes(),
                        likeCountsByCourseIds.getOrDefault(item.courseId(), 0L),
                        bookmarkCountsByCourseIds.getOrDefault(item.courseId(), 0L),
                        item.snapshotImageUrl()
                ))
                .toList();
    }
}
