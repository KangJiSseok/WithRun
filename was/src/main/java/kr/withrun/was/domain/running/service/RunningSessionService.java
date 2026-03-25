package kr.withrun.was.domain.running.service;

import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.entity.CourseDifficulty;
import kr.withrun.was.domain.course.entity.CourseGhostLeaderboard;
import kr.withrun.was.domain.course.entity.CourseTypeMap;
import kr.withrun.was.domain.course.repository.CourseDifficultyRepository;
import kr.withrun.was.domain.course.repository.CourseGhostLeaderboardRepository;
import kr.withrun.was.domain.course.repository.CourseRepository;
import kr.withrun.was.domain.course.repository.CourseTypeMapRepository;
import kr.withrun.was.domain.course.service.CourseSignalService;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.domain.course.vo.GeoPoint;
import kr.withrun.was.domain.running.dto.CompleteRunningSessionRequest;
import kr.withrun.was.domain.running.dto.CreateRunningGpsSampleRequest;
import kr.withrun.was.domain.running.dto.CreateRunningHealthSampleRequest;
import kr.withrun.was.domain.running.dto.CreateRunningSessionRequest;
import kr.withrun.was.domain.running.dto.CreateRunningSessionResponse;
import kr.withrun.was.domain.running.dto.CreateRunningSessionSplitRequest;
import kr.withrun.was.domain.running.dto.GhostRunningResultResponse;
import kr.withrun.was.domain.running.dto.PastRunningSessionItemResponse;
import kr.withrun.was.domain.running.dto.PastRunningSessionsRequest;
import kr.withrun.was.domain.running.dto.PastRunningSessionsResponse;
import kr.withrun.was.domain.running.dto.RegisterRunningSessionCourseRequest;
import kr.withrun.was.domain.running.dto.RegisterRunningSessionCourseResponse;
import kr.withrun.was.domain.running.dto.RunningGpsSampleResponse;
import kr.withrun.was.domain.running.dto.RunningHealthSampleResponse;
import kr.withrun.was.domain.running.dto.RunningSessionDetailResponse;
import kr.withrun.was.domain.running.dto.RunningSessionSplitResponse;
import kr.withrun.was.domain.running.entity.GhostRunningResult;
import kr.withrun.was.domain.running.entity.RunningGpsSample;
import kr.withrun.was.domain.running.entity.RunningHealthSample;
import kr.withrun.was.domain.running.entity.RunningSession;
import kr.withrun.was.domain.running.entity.RunningSessionSplit;
import kr.withrun.was.domain.running.repository.GhostRunningResultRepository;
import kr.withrun.was.domain.running.repository.RunningGpsSampleRepository;
import kr.withrun.was.domain.running.repository.RunningHealthSampleRepository;
import kr.withrun.was.domain.running.repository.RunningSessionRepository;
import kr.withrun.was.domain.running.repository.RunningSessionSplitRepository;
import kr.withrun.was.domain.running.repository.query.dto.PastRunningSessionHistoryRow;
import kr.withrun.was.domain.running.type.GhostResultStatus;
import kr.withrun.was.domain.running.type.RunningMode;
import kr.withrun.was.domain.running.util.PastRunningSessionCursorCodec;
import kr.withrun.was.domain.running.vo.RunningSessionDateRange;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.entity.UserCalendar;
import kr.withrun.was.domain.user.repository.UserCalendarRepository;
import kr.withrun.was.domain.user.repository.UserRepository;
import kr.withrun.was.global.common.type.Difficulty;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RunningSessionService {

    @Value("${app.cdn.base-url}")
    private String CDN_URL;

    private final RunningSessionRepository runningSessionRepository;
    private final UserRepository userRepository;
    private final UserCalendarRepository userCalendarRepository;
    private final CourseRepository courseRepository;
    private final CourseDifficultyRepository courseDifficultyRepository;
    private final CourseGhostLeaderboardRepository courseGhostLeaderboardRepository;
    private final CourseTypeMapRepository courseTypeMapRepository;
    private final RunningGpsSampleRepository runningGpsSampleRepository;
    private final RunningHealthSampleRepository runningHealthSampleRepository;
    private final RunningSessionSplitRepository runningSessionSplitRepository;
    private final GhostRunningResultRepository ghostRunningResultRepository;
    private final GhostRunningResultService ghostRunningResultService;
    private final CourseSignalService courseSignalService;

    @Transactional
    public CreateRunningSessionResponse createRunningSession(CreateRunningSessionRequest request) {
        RunningMode mode = request.mode();
        User user = getUser(request.userId());
        Course course = getCourse(mode, request.courseId());
        RunningSession ghostTargetRunningSession = getGhostTargetRunningSession(mode, request.ghostTargetRunningSessionId());

        RunningSession savedRunningSession = runningSessionRepository.save(RunningSession.start(
                user,
                mode,
                course,
                ghostTargetRunningSession,
                request.startLatitude(),
                request.startLongitude()
        ));

        List<RunningGpsSampleResponse> ghostTargetGpsSamples = getGhostTargetGpsSamples(mode, ghostTargetRunningSession);

        return CreateRunningSessionResponse.from(savedRunningSession, ghostTargetGpsSamples);
    }

    @Transactional
    public RunningSessionDetailResponse completeRunningSession(Long runningSessionId, CompleteRunningSessionRequest request) {
        RunningSession runningSession = getRunningSession(runningSessionId);
        User user = getUser(request.userId());
        runningSession.validateOwner(user.getId());
        runningSession.validateCompletable();

        runningSession.complete(
                request.distanceM(),
                request.caloriesKcal(),
                request.endLatitude(),
                request.endLongitude(),
                request.avgSpeedMps(),
                request.durationSec(),
                request.avgPaceSecPerKm(),
                request.elevationGainM()
        );
        recordCourseCompletionIfEligible(runningSession, user);
        upsertUserCalendar(runningSession);

        List<RunningGpsSample> savedGpsSamples = saveGpsSamples(runningSession, user, request.gpsSamples());
        List<RunningHealthSample> savedHealthSamples = saveHealthSamples(runningSession, user, request.healthSamples());
        List<RunningSessionSplit> savedSplits = saveSplits(runningSession, user, request.splits());

        GhostRunningResult ghostRunningResult = createGhostRunningResultIfNeeded(runningSession, request.distanceGapM());

        List<RunningGpsSampleResponse> gpsResponses = savedGpsSamples.stream()
                .map(RunningGpsSampleResponse::from)
                .toList();
        List<RunningHealthSampleResponse> healthResponses = savedHealthSamples.stream()
                .map(RunningHealthSampleResponse::from)
                .toList();
        List<RunningSessionSplitResponse> splitResponses = savedSplits.stream()
                .map(RunningSessionSplitResponse::from)
                .toList();

        GhostRunningResultResponse ghostRunningResultResponse = ghostRunningResult == null
                ? null
                : toGhostRunningResultResponse(ghostRunningResult);

        return RunningSessionDetailResponse.of(
                runningSession,
                gpsResponses,
                healthResponses,
                splitResponses,
                CDN_URL + "/" + runningSession.getSnapshotImageUrl(),
                ghostRunningResultResponse
        );
    }

    @Transactional
    public RegisterRunningSessionCourseResponse registerRunningSessionCourse(
            Long runningSessionId,
            RegisterRunningSessionCourseRequest request
    ) {
        RunningSession runningSession = getRunningSession(runningSessionId);
        runningSession.validateCourseRegistrable();
        CourseStatus courseStatus = parseCourseStatus(request.mode());
        Difficulty difficulty = parseDifficulty(request.difficulty());
        List<CourseType> courseTypes = parseCourseTypes(request.courseTypes());
        Coordinates courseCoordinates = buildCourseCoordinates(runningSessionId);

        Course course = courseRepository.save(Course.builder()
                .title(request.title())
                .status(courseStatus)
                .distanceM(runningSession.getDistanceM())
                .elevationGainM(runningSession.getElevationGainM())
                .snapshotImageUrl(resolveSnapshotImageUrl(request.snapshotImageUrl(), runningSession.getSnapshotImageUrl()))
                .startLatitude(runningSession.getStartLatitude())
                .startLongitude(runningSession.getStartLongitude())
                .endLatitude(runningSession.getEndLatitude())
                .endLongitude(runningSession.getEndLongitude())
                .coordinates(courseCoordinates)
                .user(runningSession.getUser())
                .build());

        courseDifficultyRepository.save(CourseDifficulty.create(course, difficulty));
        courseTypeMapRepository.saveAll(courseTypes.stream()
                .map(courseType -> CourseTypeMap.create(course, courseType))
                .toList());
        courseSignalService.upsertCourseFeature(course, difficulty, courseTypes);

        return RegisterRunningSessionCourseResponse.from(
                course,
                difficulty,
                CDN_URL + "/" + course.getSnapshotImageUrl()
        );
    }

    public RunningSessionDetailResponse findRunningSessionDetail(Long runningSessionId, Long userId) {
        RunningSession runningSession = getRunningSession(runningSessionId);
        runningSession.validateOwner(userId);

        List<RunningGpsSampleResponse> gpsSamples = runningGpsSampleRepository.findByRunningSessionIdOrderBySampledAtAsc(runningSessionId)
                .stream()
                .map(RunningGpsSampleResponse::from)
                .toList();
        List<RunningHealthSampleResponse> healthSamples = runningHealthSampleRepository.findByRunningSessionIdOrderBySampledAtAsc(runningSessionId)
                .stream()
                .map(RunningHealthSampleResponse::from)
                .toList();
        List<RunningSessionSplitResponse> splits = runningSessionSplitRepository.findByRunningSessionIdOrderBySplitIndexAsc(runningSessionId)
                .stream()
                .map(RunningSessionSplitResponse::from)
                .toList();

        GhostRunningResultResponse ghostRunningResult = ghostRunningResultRepository.findByRunningSessionIdAndDeletedAtIsNull(runningSessionId)
                .map(this::toGhostRunningResultResponse)
                .orElse(null);

        return RunningSessionDetailResponse.of(
                runningSession,
                gpsSamples,
                healthSamples,
                splits,
                CDN_URL + "/" + runningSession.getSnapshotImageUrl(),
                ghostRunningResult
        );
    }

    public PastRunningSessionsResponse findPastRunningSessions(PastRunningSessionsRequest request) {
        PastRunningSessionCursorCodec.CursorPayload payload = request.cursor() == null
                ? null
                : PastRunningSessionCursorCodec.decode(request.cursor());

        RunningSessionDateRange dateRange = RunningSessionDateRange.of(request.year(), request.month(), request.day());

        getUser(request.userId());

        List<PastRunningSessionHistoryRow> rows = runningSessionRepository.findPastRunningSessionHistoryRows(
                request.userId(),
                dateRange.startInclusive(),
                dateRange.endExclusive(),
                payload == null ? null : payload.startedAt(),
                payload == null ? null : payload.runningSessionId(),
                request.pageSize()
        );

        boolean hasMore = rows.size() > request.pageSize();
        List<PastRunningSessionHistoryRow> pagedRows = hasMore
                ? List.copyOf(rows.subList(0, request.pageSize()))
                : List.copyOf(rows);
        List<PastRunningSessionItemResponse> items = pagedRows.stream()
                .map(row -> PastRunningSessionItemResponse.from(row, CDN_URL + "/" + row.snapshotImageUrl()))
                .toList();
        String nextCursor = hasMore ? toCursor(pagedRows.getLast()) : null;

        return new PastRunningSessionsResponse(items, hasMore, nextCursor);
    }

    private GhostRunningResult createGhostRunningResultIfNeeded(RunningSession runningSession, Integer requestedDistanceGapM) {
        if (runningSession.getMode() != RunningMode.GHOST) {
            return null;
        }

        RunningSession targetSession = runningSession.getGhostTargetRunningSession();
        if (targetSession == null || !Boolean.TRUE.equals(targetSession.getIsCompleted())) {
            throw new CustomException(ResponseCode.INVALID_GHOST_TARGET);
        }

        if (runningSession.getDurationSec() == null || targetSession.getDurationSec() == null) {
            throw new CustomException(ResponseCode.INVALID_INPUT_VALUE);
        }

        Difficulty difficulty = resolveGhostDifficulty(runningSession, targetSession);
        int point = ghostRunningResultService.calculateGhostRunningPoint(runningSession.getDurationSec(), difficulty);
        int targetPoint = ghostRunningResultService.calculateGhostRunningPoint(targetSession.getDurationSec(), difficulty);

        GhostResultStatus resultStatus = resolveGhostResultStatus(point, targetPoint);
        int timeGapSec = Math.abs(runningSession.getDurationSec() - targetSession.getDurationSec());

        if (requestedDistanceGapM == null) {
            throw new CustomException(ResponseCode.INVALID_INPUT_VALUE);
        }
        int distanceGapM = requestedDistanceGapM;

        GhostRunningResult ghostRunningResult = GhostRunningResult.create(
                resultStatus,
                point,
                timeGapSec,
                distanceGapM,
                runningSession,
                targetSession,
                targetSession.getUser()
        );

        GhostRunningResult savedGhostRunningResult = ghostRunningResultRepository.save(ghostRunningResult);
        upsertCourseGhostLeaderboard(runningSession, point);
        return savedGhostRunningResult;
    }

    private void upsertUserCalendar(RunningSession runningSession) {
        LocalDate calendarDate = runningSession.getStartedAt().toLocalDate();
        UserCalendar userCalendar = userCalendarRepository.findByUserIdAndCalendarDateAndDeletedAtIsNull(
                        runningSession.getUser().getId(),
                        calendarDate
                )
                .orElseGet(() -> UserCalendar.create(runningSession.getUser(), calendarDate));

        userCalendar.recordCompletedRun(
                runningSession.getMode(),
                runningSession.getDistanceM(),
                runningSession.getDurationSec(),
                runningSession.getCaloriesKcal(),
                runningSession.getSnapshotImageUrl()
        );
        userCalendarRepository.save(userCalendar);
    }

    private Difficulty resolveGhostDifficulty(RunningSession runningSession, RunningSession targetSession) {
        Long courseId = runningSession.getCourse() != null
                ? runningSession.getCourse().getId()
                : targetSession.getCourse() == null ? null : targetSession.getCourse().getId();

        if (courseId == null) {
            throw new CustomException(ResponseCode.COURSE_NOT_FOUND);
        }

        return courseDifficultyRepository.findDifficultyByCourseId(courseId)
                .orElseThrow(() -> new CustomException(ResponseCode.COURSE_DIFFICULTY_NOT_FOUND));
    }

    private GhostResultStatus resolveGhostResultStatus(int point, int targetPoint) {
        if (point > targetPoint) {
            return GhostResultStatus.WIN;
        }
        if (point < targetPoint) {
            return GhostResultStatus.LOSE;
        }
        return GhostResultStatus.DRAW;
    }

    private void upsertCourseGhostLeaderboard(RunningSession runningSession, int point) {
        Course leaderboardCourse = resolveLeaderboardCourse(runningSession);
        CourseGhostLeaderboard existingRow = courseGhostLeaderboardRepository
                .findTopByUserIdAndCourseId(
                        runningSession.getUser().getId(),
                        leaderboardCourse.getId()
                )
                .orElse(null);

        if (existingRow == null) {
            courseGhostLeaderboardRepository.save(CourseGhostLeaderboard.create(
                    runningSession.getUser(),
                    leaderboardCourse,
                    runningSession,
                    point
            ));
            return;
        }

        int existingPoint = existingRow.getPoint() == null ? 0 : existingRow.getPoint();
        if (point > existingPoint) {
            existingRow.updatePointAndRunningSession(point, runningSession);
        }
    }

    private Course resolveLeaderboardCourse(RunningSession runningSession) {
        if (runningSession.getCourse() != null) {
            return runningSession.getCourse();
        }

        RunningSession targetSession = runningSession.getGhostTargetRunningSession();
        if (targetSession != null && targetSession.getCourse() != null) {
            return targetSession.getCourse();
        }

        throw new CustomException(ResponseCode.COURSE_NOT_FOUND);
    }

    private List<RunningGpsSample> saveGpsSamples(
            RunningSession runningSession,
            User user,
            List<CreateRunningGpsSampleRequest> requests
    ) {
        List<RunningGpsSample> samples = requests.stream()
                .map(request -> RunningGpsSample.create(
                        user,
                        runningSession,
                        request.bearingDeg(),
                        request.accuracyM(),
                        request.altitudeM(),
                        request.longitude(),
                        request.latitude(),
                        request.speedMps(),
                        request.paceSecPerKm(),
                        request.distanceM(),
                        request.cadenceSpm(),
                        request.sampledAt()
                ))
                .toList();
        return runningGpsSampleRepository.saveAll(samples);
    }

    private List<RunningHealthSample> saveHealthSamples(
            RunningSession runningSession,
            User user,
            List<CreateRunningHealthSampleRequest> requests
    ) {
        List<RunningHealthSample> samples = requests.stream()
                .map(request -> RunningHealthSample.create(
                        user,
                        runningSession,
                        request.heartRate(),
                        request.caloriesKcal(),
                        request.sampledAt()
                ))
                .toList();
        return runningHealthSampleRepository.saveAll(samples);
    }

    private List<RunningSessionSplit> saveSplits(
            RunningSession runningSession,
            User user,
            List<CreateRunningSessionSplitRequest> requests
    ) {
        List<RunningSessionSplit> splits = requests.stream()
                .map(request -> {
                    if (!user.getId().equals(request.userId())) {
                        throw new CustomException(ResponseCode.INVALID_INPUT_VALUE);
                    }
                    return RunningSessionSplit.create(
                            runningSession,
                            request.splitIndex(),
                            request.splitDistanceM(),
                            request.splitDurationSec(),
                            request.splitPaceSecPerKm(),
                            request.avgHeartRate(),
                            request.elevationGainM()
                    );
                })
                .toList();
        return runningSessionSplitRepository.saveAll(splits);
    }

    private GhostRunningResultResponse toGhostRunningResultResponse(GhostRunningResult ghostRunningResult) {
        return new GhostRunningResultResponse(
                ghostRunningResult.getId(),
                ghostRunningResult.getRunningSession().getId(),
                ghostRunningResult.getGhostTargetRunningSession().getId(),
                ghostRunningResult.getTargetUser() == null ? null : ghostRunningResult.getTargetUser().getId(),
                ghostRunningResult.getResultStatus(),
                ghostRunningResult.getPoint(),
                ghostRunningResult.getTimeGapSec(),
                ghostRunningResult.getDistanceGapM(),
                ghostRunningResult.getCreatedAt()
        );
    }

    private User getUser(Long userId) {
        return userRepository.findNotDeletedUser(userId)
                .orElseThrow(() -> new CustomException(ResponseCode.USER_NOT_FOUND));
    }

    private List<RunningGpsSampleResponse> getGhostTargetGpsSamples(
            RunningMode mode,
            RunningSession ghostTargetRunningSession
    ) {
        if (mode != RunningMode.GHOST || ghostTargetRunningSession == null) {
            return null;
        }

        return runningGpsSampleRepository.findByRunningSessionIdOrderBySampledAtAsc(ghostTargetRunningSession.getId())
                .stream()
                .map(RunningGpsSampleResponse::from)
                .toList();
    }

    private Course getCourse(RunningMode mode, Long courseId) {
        if (mode == RunningMode.FREE) {
            return null;
        }

        if (courseId == null) {
            throw new CustomException(ResponseCode.INVALID_INPUT_VALUE);
        }

        return courseRepository.findNotDeletedCourse(courseId)
                .filter(course -> course.getStatus() == CourseStatus.OFFICIAL)
                .orElseThrow(() -> new CustomException(ResponseCode.COURSE_NOT_FOUND));
    }

    private RunningSession getGhostTargetRunningSession(RunningMode mode, Long ghostTargetRunningSessionId) {
        if (mode != RunningMode.GHOST) {
            return null;
        }

        if (ghostTargetRunningSessionId == null) {
            return null;
        }

        RunningSession ghostTargetRunningSession = runningSessionRepository.findByIdAndDeletedAtIsNull(ghostTargetRunningSessionId)
                .orElseThrow(() -> new CustomException(ResponseCode.GHOST_TARGET_NOT_FOUND));

        if (!Boolean.TRUE.equals(ghostTargetRunningSession.getIsCompleted())) {
            throw new CustomException(ResponseCode.INVALID_GHOST_TARGET);
        }

        return ghostTargetRunningSession;
    }

    private RunningSession getRunningSession(Long runningSessionId) {
        return runningSessionRepository.findByIdAndDeletedAtIsNull(runningSessionId)
                .orElseThrow(() -> new CustomException(ResponseCode.RUNNING_SESSION_NOT_FOUND));
    }

    private Difficulty parseDifficulty(String rawDifficulty) {
        try {
            return Difficulty.valueOf(rawDifficulty);
        } catch (IllegalArgumentException exception) {
            throw new CustomException(ResponseCode.INVALID_INPUT_VALUE);
        }
    }

    private CourseStatus parseCourseStatus(String rawCourseStatus) {
        try {
            CourseStatus courseStatus = CourseStatus.valueOf(rawCourseStatus);
            if (courseStatus == CourseStatus.COMMUNITY || courseStatus == CourseStatus.PRIVATE) {
                return courseStatus;
            }
        } catch (IllegalArgumentException exception) {
            throw new CustomException(ResponseCode.INVALID_INPUT_VALUE);
        }

        throw new CustomException(ResponseCode.INVALID_INPUT_VALUE);
    }

    private List<CourseType> parseCourseTypes(List<String> rawCourseTypes) {
        LinkedHashSet<CourseType> courseTypes = new LinkedHashSet<>();
        for (String rawCourseType : rawCourseTypes) {
            courseTypes.add(parseCourseType(rawCourseType));
        }
        return List.copyOf(courseTypes);
    }

    private CourseType parseCourseType(String rawCourseType) {
        try {
            return CourseType.valueOf(rawCourseType);
        } catch (IllegalArgumentException exception) {
            throw new CustomException(ResponseCode.INVALID_INPUT_VALUE);
        }
    }

    private String resolveSnapshotImageUrl(String requestedSnapshotImageUrl, String runningSessionSnapshotImageUrl) {
        if (requestedSnapshotImageUrl == null || requestedSnapshotImageUrl.isBlank()) {
            return runningSessionSnapshotImageUrl;
        }
        return requestedSnapshotImageUrl;
    }

    private Coordinates buildCourseCoordinates(Long runningSessionId) {
        List<GeoPoint> points = runningGpsSampleRepository.findByRunningSessionIdOrderBySampledAtAsc(runningSessionId)
                .stream()
                .map(sample -> new GeoPoint(sample.getLatitude(), sample.getLongitude(), sample.getAltitudeM()))
                .toList();
        return new Coordinates(points);
    }

    private String toCursor(PastRunningSessionHistoryRow row) {
        return PastRunningSessionCursorCodec.encode(row.startedAt(), row.runningSessionId());
    }

    private void recordCourseCompletionIfEligible(RunningSession runningSession, Long userId) {
        if (runningSession.getCourse() == null || userId == null) {
            return;
        }
        User user = getUser(userId);
        recordCourseCompletionIfEligible(runningSession, user);
    }

    private void recordCourseCompletionIfEligible(RunningSession runningSession, User user) {
        if (runningSession.getCourse() == null || user == null) {
            return;
        }
        courseSignalService.recordCompletion(
                runningSession.getCourse(),
                user,
                runningSession.getId()
        );
    }
}
