package kr.withrun.was.domain.course.service;

import kr.withrun.was.domain.course.dto.CourseDetailResponse;
import kr.withrun.was.domain.course.dto.CourseLeaderboardItemResponse;
import kr.withrun.was.domain.course.dto.CourseLeaderboardRequest;
import kr.withrun.was.domain.course.dto.CourseLeaderboardResponse;
import kr.withrun.was.domain.course.dto.CourseSurveyResponse;
import kr.withrun.was.domain.course.dto.NearbyCourseItemResponse;
import kr.withrun.was.domain.course.dto.NearbyCoursesRequest;
import kr.withrun.was.domain.course.dto.NearbyCoursesResponse;
import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.repository.CourseGhostLeaderboardRepository;
import kr.withrun.was.domain.course.repository.CourseDifficultyRepository;
import kr.withrun.was.domain.course.repository.CourseLikeRepository;
import kr.withrun.was.domain.course.repository.CourseRepository;
import kr.withrun.was.domain.course.repository.CourseReviewRepository;
import kr.withrun.was.domain.course.repository.CourseTypeMapRepository;
import kr.withrun.was.domain.course.repository.query.dto.CourseGhostLeaderboardRankRow;
import kr.withrun.was.domain.course.repository.query.dto.NearbyCourseCandidateRow;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.domain.course.util.HaversineDistanceCalculator;
import kr.withrun.was.domain.course.util.NearbyCourseCursorCodec;
import kr.withrun.was.global.common.type.Difficulty;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CourseService {

    // 목표 거리 허용 오차는 +- 1km로 두고 비슷한 길이의 코스만 후보로 남긴다.
    private static final int TARGET_DISTANCE_TOLERANCE_M = 1000;
    // 요청 페이지 크기가 없으면 기본으로 3개를 내려준다.
    private static final int DEFAULT_PAGE_SIZE = 3;
    // 반경은 최대 5km까지만 점진적으로 확장한다.
    private static final int MAX_RADIUS_M = 5000;
    private static final int RADIUS_STEP_M = 1000;

    private final CourseRepository courseRepository;
    private final CourseDifficultyRepository courseDifficultyRepository;
    private final CourseTypeMapRepository courseTypeMapRepository;
    private final CourseLikeRepository courseLikeRepository;
    private final CourseReviewRepository courseReviewRepository;
    private final CourseGhostLeaderboardRepository courseGhostLeaderboardRepository;

    /**
     * 주변 코스 목록을 커서 기반으로 잘라 반환한다.
     * <p>
     * 전체 후보를 거리순으로 정렬한 뒤 커서 이후 데이터만 잘라서 현재 페이지를 만들고,
     * 다음 페이지가 없으면 클라이언트가 탐색 범위를 넓힐 수 있도록 다음 추천 반경도 함께 내려준다.
     */
    public NearbyCoursesResponse findNearbyCourses(NearbyCoursesRequest request) {
        int pageSize = resolvePageSize(request.pageSize());
        List<NearbyCourseItemResponse> sortedItems = findNearbyCourseItems(request);
        int startIndex = resolveStartIndex(sortedItems, request.cursor());
        List<NearbyCourseItemResponse> remainingItems = sortedItems.subList(startIndex, sortedItems.size());
        boolean hasMore = remainingItems.size() > pageSize;
        List<NearbyCourseItemResponse> pageItems = List.copyOf(remainingItems.subList(0, Math.min(pageSize, remainingItems.size())));
        String nextCursor = hasMore ? toCursor(pageItems.get(pageItems.size() - 1)) : null;
        Integer suggestedNextRadiusM = resolveSuggestedNextRadiusM(request.radiusM(), hasMore);

        return new NearbyCoursesResponse(pageItems, hasMore, nextCursor, suggestedNextRadiusM);
    }

    public CourseSurveyResponse findCourseSurveys() {
        return new CourseSurveyResponse(
                Arrays.stream(CourseType.values())
                        .map(courseType -> new CourseSurveyResponse.CourseSurveyListResponse(
                                courseType.getIndex(),
                                courseType.getLabel()
                        ))
                        .toList(),
                Arrays.stream(Difficulty.values())
                        .map(difficulty -> new CourseSurveyResponse.CourseSurveyListResponse(
                                difficulty.getIndex(),
                                difficulty.getLabel()
                        ))
                        .toList()
        );
    }

    public CourseDetailResponse findCourseDetail(Long courseId) {
        Course course = getCourse(courseId);

        return new CourseDetailResponse(
                course.getId(),
                course.getTitle(),
                course.getStatus(),
                courseDifficultyRepository.findDifficultyByCourseId(courseId)
                        .orElse(null),
                course.getDistanceM(),
                course.getElevationGainM(),
                course.getSnapshotImageUrl(),
                course.getStartLatitude(),
                course.getStartLongitude(),
                course.getEndLatitude(),
                course.getEndLongitude(),
                course.getCoordinates(),
                courseTypeMapRepository.findCourseTypesByCourseId(courseId),
                courseLikeRepository.countByCourseId(courseId),
                // TODO: 인증에서 userId를 전달받는 방식이 정해지면 isLiked/isBookmarked를 사용자 기준으로 계산하도록 수정해야 한다.
                false,
                false,
                courseReviewRepository.findAverageRatingByCourseId(courseId)
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

    private Course getCourse(Long courseId) {
        Course course = courseRepository.findNotDeletedCourse(courseId)
                .orElseThrow(() -> new CustomException(ResponseCode.COURSE_NOT_FOUND));
        return course;
    }


    private int resolvePageSize(Integer pageSize) {
        return pageSize == null ? DEFAULT_PAGE_SIZE : pageSize;
    }

    /**
     * DB에서 읽어온 후보 행을 코스 단위로 묶어 실제 응답 아이템으로 변환한다.
     * <p>
     * 한 코스가 여러 course type 행으로 조회될 수 있으므로 먼저 courseId 기준으로 그룹핑하고,
     * 사용자와 가장 가까운 지점 거리, 목표 거리 적합 여부, 현재 탐색 반경을 순서대로 적용한다.
     */
    public List<NearbyCourseItemResponse> findNearbyCourseItems(NearbyCoursesRequest request) {
        return courseRepository.findOfficialNearbyCourseCandidates().stream()
                .filter(candidate -> candidate.courseId() != null)
                .collect(Collectors.groupingBy(NearbyCourseCandidateRow::courseId))
                .values().stream()
                .map(candidateRows -> toNearbyCourseItem(candidateRows, request.latitude(), request.longitude()))
                .flatMap(Optional::stream)
                .filter(item -> isWithinTargetDistance(item.distanceM(), request.targetDistanceM()))
                .filter(item -> item.distanceFromUserM() <= request.radiusM())
                .sorted(
                        Comparator.comparingInt(NearbyCourseItemResponse::distanceFromUserM)
                                .thenComparingLong(NearbyCourseItemResponse::courseId)
                )
                .toList();
    }

    // 커서는 "마지막으로 본 코스"를 의미하므로, 해당 코스보다 뒤에 오는 첫 번째 인덱스를 찾는다.
    private int resolveStartIndex(List<NearbyCourseItemResponse> sortedItems, String cursor) {
        if (cursor == null) {
            return 0;
        }

        NearbyCourseCursorCodec.CursorPayload payload = NearbyCourseCursorCodec.decode(cursor);
        for (int index = 0; index < sortedItems.size(); index++) {
            if (isStrictlyAfter(sortedItems.get(index), payload)) {
                return index;
            }
        }

        return sortedItems.size();
    }

    // 정렬 기준이 사용자와의 거리 -> courseId 이므로 같은 기준으로 "다음 데이터" 여부를 판단한다.
    private boolean isStrictlyAfter(
            NearbyCourseItemResponse item,
            NearbyCourseCursorCodec.CursorPayload payload
    ) {
        return item.distanceFromUserM() > payload.lastDistanceFromUserM()
                || (item.distanceFromUserM() == payload.lastDistanceFromUserM()
                && item.courseId() > payload.lastCourseId());
    }

    private String toCursor(NearbyCourseItemResponse item) {
        return NearbyCourseCursorCodec.encode(item.distanceFromUserM(), item.courseId());
    }

    // 더 보여줄 데이터가 없고 아직 최대 반경에 도달하지 않았다면 다음 탐색 반경을 제안한다.
    private Integer resolveSuggestedNextRadiusM(int radiusM, boolean hasMore) {
        if (hasMore || radiusM >= MAX_RADIUS_M) {
            return null;
        }

        return Math.min(radiusM + RADIUS_STEP_M, MAX_RADIUS_M);
    }

    /**
     * 동일 코스에 속한 후보 행들 중 대표 정보를 뽑아 응답 객체를 만든다.
     * <p>
     * 코스 좌표 전체를 기준으로 사용자의 현재 위치와 가장 가까운 지점을 계산하고,
     * 그 값이 있어야만 주변 코스로 노출한다.
     */
    private Optional<NearbyCourseItemResponse> toNearbyCourseItem(
            List<NearbyCourseCandidateRow> candidateRows,
            double userLatitude,
            double userLongitude
    ) {
        NearbyCourseCandidateRow representative = candidateRows.getFirst();
        OptionalInt distanceFromUserM = HaversineDistanceCalculator.calculateMinimumDistanceMeters(
                representative.coordinates(),
                userLatitude,
                userLongitude
        );
        if (distanceFromUserM.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(
                new NearbyCourseItemResponse(
                        representative.courseId(),
                        representative.title(),
                        representative.status(),
                        representative.distanceM(),
                        representative.elevationGainM(),
                        representative.startLatitude(),
                        representative.startLongitude(),
                        representative.endLatitude(),
                        representative.endLongitude(),
                        distanceFromUserM.getAsInt(),
                        representative.difficulty(),
                        mergeCourseTypes(candidateRows),
                        representative.snapshotImageUrl()
                )
        );
    }

    // 목표 거리와 완전히 일치하는 코스만 찾으면 후보가 너무 적어질 수 있어 허용 오차를 둔다.
    private boolean isWithinTargetDistance(Integer courseDistanceM, int targetDistanceM) {
        if (courseDistanceM == null) {
            return false;
        }

        int minimumDistanceM = targetDistanceM - TARGET_DISTANCE_TOLERANCE_M;
        int maximumDistanceM = targetDistanceM + TARGET_DISTANCE_TOLERANCE_M;
        return courseDistanceM >= minimumDistanceM && courseDistanceM <= maximumDistanceM;
    }

    // 같은 코스가 여러 타입으로 조회될 수 있으므로 중복 없이 하나의 리스트로 합친다.
    private List<CourseType> mergeCourseTypes(List<NearbyCourseCandidateRow> candidateRows) {
        EnumSet<CourseType> courseTypes = EnumSet.noneOf(CourseType.class);
        for (NearbyCourseCandidateRow candidateRow : candidateRows) {
            if (candidateRow.courseType() != null) {
                courseTypes.add(candidateRow.courseType());
            }
        }
        return List.copyOf(courseTypes);
    }
}
