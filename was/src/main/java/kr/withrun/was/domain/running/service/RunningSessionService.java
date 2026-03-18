package kr.withrun.was.domain.running.service;

import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.entity.CourseDifficulty;
import kr.withrun.was.domain.course.entity.CourseTypeMap;
import kr.withrun.was.domain.course.repository.CourseDifficultyRepository;
import kr.withrun.was.domain.course.repository.CourseRepository;
import kr.withrun.was.domain.course.repository.CourseTypeMapRepository;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.domain.course.vo.GeoPoint;
import kr.withrun.was.domain.running.dto.CreateRunningSessionRequest;
import kr.withrun.was.domain.running.dto.CreateRunningSessionResponse;
import kr.withrun.was.domain.running.dto.GhostRunningResultResponse;
import kr.withrun.was.domain.running.dto.PastRunningSessionItemResponse;
import kr.withrun.was.domain.running.dto.PastRunningSessionsRequest;
import kr.withrun.was.domain.running.dto.PastRunningSessionsResponse;
import kr.withrun.was.domain.running.dto.RegisterRunningSessionCourseRequest;
import kr.withrun.was.domain.running.dto.RegisterRunningSessionCourseResponse;
import kr.withrun.was.domain.running.dto.UpdateRunningSessionRequest;
import kr.withrun.was.domain.running.dto.UpdateRunningSessionResponse;
import kr.withrun.was.domain.running.entity.RunningSession;
import kr.withrun.was.domain.running.repository.GhostRunningResultRepository;
import kr.withrun.was.domain.running.repository.RunningSessionRepository;
import kr.withrun.was.domain.running.repository.query.dto.PastRunningSessionHistoryRow;
import kr.withrun.was.domain.running.type.RunningMode;
import kr.withrun.was.domain.running.util.PastRunningSessionCursorCodec;
import kr.withrun.was.domain.running.vo.RunningSessionDateRange;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.repository.UserRepository;
import kr.withrun.was.global.common.type.Difficulty;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RunningSessionService {

    private final GhostRunningResultRepository ghostRunningResultRepository;
    private final RunningSessionRepository runningSessionRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CourseDifficultyRepository courseDifficultyRepository;
    private final CourseTypeMapRepository courseTypeMapRepository;

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

        return CreateRunningSessionResponse.from(savedRunningSession);
    }

    @Transactional
    public UpdateRunningSessionResponse completeRunningSession(Long runningSessionId, UpdateRunningSessionRequest request) {
        RunningSession runningSession = getRunningSession(runningSessionId);
        runningSession.validateOwner(request.userId());
        runningSession.validateCompletable();

        runningSession.complete(
                request.endedAt(),
                request.distanceM(),
                request.caloriesKcal(),
                request.endLatitude(),
                request.endLongitude(),
                request.avgSpeedMps(),
                request.durationSec(),
                request.avgPaceSecPerKm(),
                request.elevationGainM()
        );

        return UpdateRunningSessionResponse.from(runningSession);
    }

    @Transactional
    public RegisterRunningSessionCourseResponse registerRunningSessionCourse(
            Long runningSessionId,
            RegisterRunningSessionCourseRequest request
    ) {
        RunningSession runningSession = getRunningSession(runningSessionId);
        runningSession.validateCourseRegistrable();
        Difficulty difficulty = parseDifficulty(request.difficulty());
        List<CourseType> courseTypes = parseCourseTypes(request.courseTypes());

        Course course = courseRepository.save(Course.builder()
                .title(request.title())
                .status(CourseStatus.COMMUNITY)
                .distanceM(runningSession.getDistanceM())
                .elevationGainM(runningSession.getElevationGainM())
                .snapshotImageUrl(resolveSnapshotImageUrl(request.snapshotImageUrl(), runningSession.getSnapshotImageUrl()))
                .startLatitude(runningSession.getStartLatitude())
                .startLongitude(runningSession.getStartLongitude())
                .endLatitude(runningSession.getEndLatitude())
                .endLongitude(runningSession.getEndLongitude())
                .coordinates(copyCoordinates(runningSession.getCoordinates()))
                .user(runningSession.getUser())
                .build());

        courseDifficultyRepository.save(CourseDifficulty.create(course, difficulty));
        courseTypeMapRepository.saveAll(courseTypes.stream()
                .map(courseType -> CourseTypeMap.create(course, courseType))
                .toList());

        return RegisterRunningSessionCourseResponse.from(course, difficulty);
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
                .map(PastRunningSessionItemResponse::from)
                .toList();
        String nextCursor = hasMore ? toCursor(pagedRows.getLast()) : null;

        return new PastRunningSessionsResponse(items, hasMore, nextCursor);
    }

    public GhostRunningResultResponse findGhostRunningResult(Long runningSessionId) {
        return ghostRunningResultRepository.findByRunningSessionId(runningSessionId)
                .map(GhostRunningResultResponse::from)
                .orElseThrow(() -> new CustomException(ResponseCode.GHOST_RUNNING_RESULT_NOT_FOUND));
    }

    private User getUser(Long userId) {
        return userRepository.findNotDeletedUser(userId)
                .orElseThrow(() -> new CustomException(ResponseCode.USER_NOT_FOUND));
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
            throw new CustomException(ResponseCode.INVALID_INPUT_VALUE);
        }

        RunningSession ghostTargetRunningSession = runningSessionRepository.findByIdAndDeletedAtIsNull(ghostTargetRunningSessionId)
                .orElseThrow(() -> new CustomException(ResponseCode.GHOST_TARGET_NOT_FOUND));

        if (!Boolean.TRUE.equals(ghostTargetRunningSession.getIsCompleted())) {
            throw new CustomException(ResponseCode.INVALID_GHOST_TARGET);
        }

        return ghostTargetRunningSession;
    }

    private RunningSession getRunningSession(Long runningSessionId) {
        RunningSession runningSession = runningSessionRepository.findByIdAndDeletedAtIsNull(runningSessionId)
                .orElseThrow(() -> new CustomException(ResponseCode.RUNNING_SESSION_NOT_FOUND));
        return runningSession;
    }

    private Difficulty parseDifficulty(String rawDifficulty) {
        try {
            return Difficulty.valueOf(rawDifficulty);
        } catch (IllegalArgumentException exception) {
            throw new CustomException(ResponseCode.INVALID_INPUT_VALUE);
        }
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

    private Coordinates copyCoordinates(Coordinates coordinates) {
        if (coordinates == null) {
            return null;
        }

        List<GeoPoint> copiedPoints = coordinates.values().stream()
                .map(point -> point == null
                        ? null
                        : new GeoPoint(point.latitude(), point.longitude(), point.elevationM()))
                .toList();
        return new Coordinates(copiedPoints);
    }

    private String toCursor(PastRunningSessionHistoryRow row) {
        return PastRunningSessionCursorCodec.encode(row.startedAt(), row.runningSessionId());
    }

}
