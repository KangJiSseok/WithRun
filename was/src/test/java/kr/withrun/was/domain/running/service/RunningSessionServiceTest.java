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
import kr.withrun.was.domain.running.repository.query.dto.GhostRunningResultRow;
import kr.withrun.was.domain.running.repository.query.dto.PastRunningSessionHistoryRow;
import kr.withrun.was.domain.running.type.GhostResultStatus;
import kr.withrun.was.domain.running.type.RunningMode;
import kr.withrun.was.domain.running.vo.RunningSessionDateRange;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.repository.UserRepository;
import kr.withrun.was.domain.user.type.Gender;
import kr.withrun.was.global.common.type.TimeSlot;
import kr.withrun.was.global.common.type.Difficulty;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static kr.withrun.was.domain.running.type.RunningMode.COURSE;
import static kr.withrun.was.domain.running.type.RunningMode.FREE;
import static kr.withrun.was.domain.running.type.RunningMode.GHOST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("러닝 세션 서비스")
class RunningSessionServiceTest {

    private static final LocalDateTime STARTED_AT = LocalDateTime.of(2026, 3, 12, 6, 30);

    @Mock
    private GhostRunningResultRepository ghostRunningResultRepository;

    @Mock
    private RunningSessionRepository runningSessionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CourseDifficultyRepository courseDifficultyRepository;

    @Mock
    private CourseTypeMapRepository courseTypeMapRepository;

    private RunningSessionService runningSessionService;

    @BeforeEach
    void setUp() {
        runningSessionService = new RunningSessionService(
                ghostRunningResultRepository,
                runningSessionRepository,
                userRepository,
                courseRepository,
                courseDifficultyRepository,
                courseTypeMapRepository
        );
    }

    @DisplayName("서비스는 클래스 기본 readOnly와 쓰기 메서드 트랜잭션을 함께 사용한다")
    @Test
    void usesReadOnlyAtClassLevelAndWritableMethods() throws NoSuchMethodException {
        Transactional classTransactional = RunningSessionService.class.getAnnotation(Transactional.class);
        Transactional createTransactional = RunningSessionService.class
                .getDeclaredMethod("createRunningSession", CreateRunningSessionRequest.class)
                .getAnnotation(Transactional.class);
        Transactional completeTransactional = RunningSessionService.class
                .getDeclaredMethod("completeRunningSession", Long.class, UpdateRunningSessionRequest.class)
                .getAnnotation(Transactional.class);

        assertThat(classTransactional).isNotNull();
        assertThat(classTransactional.readOnly()).isTrue();
        assertThat(createTransactional).isNotNull();
        assertThat(createTransactional.readOnly()).isFalse();
        assertThat(completeTransactional).isNotNull();
        assertThat(completeTransactional.readOnly()).isFalse();
    }

    @DisplayName("FREE 모드는 코스와 고스트 대상 없이 세션을 생성한다")
    @Test
    void createsFreeRunningSessionWithoutCourseOrGhostTarget() {
        User user = user(1L);
        stubUser(user);
        when(runningSessionRepository.save(any(RunningSession.class))).thenAnswer(invocation -> persist(invocation.getArgument(0), 901L));

        CreateRunningSessionResponse response = runningSessionService.createRunningSession(
                new CreateRunningSessionRequest(1L, FREE, null, null, STARTED_AT, 30.0, 176.0)
        );

        ArgumentCaptor<RunningSession> sessionCaptor = ArgumentCaptor.forClass(RunningSession.class);
        verify(runningSessionRepository, times(1)).save(sessionCaptor.capture());
        RunningSession savedSession = sessionCaptor.getValue();

        assertThat(savedSession.getUser()).isEqualTo(user);
        assertThat(savedSession.getCourse()).isNull();
        assertThat(savedSession.getGhostTargetRunningSession()).isNull();
        assertThat(savedSession.getMode()).isEqualTo(FREE);
        assertThat(savedSession.getStartedAt()).isEqualTo(STARTED_AT);
        assertThat(savedSession.getDistanceM()).isZero();
        assertThat(savedSession.getCaloriesKcal()).isZero();
        assertThat(savedSession.getIsCompleted()).isFalse();

        assertThat(response.runningSessionId()).isEqualTo(901L);
        assertThat(response.mode()).isEqualTo(FREE);
        assertThat(response.courseId()).isNull();
        assertThat(response.ghostTargetRunningSessionId()).isNull();
        assertThat(response.distanceM()).isZero();
        assertThat(response.caloriesKcal()).isZero();
        assertThat(response.isCompleted()).isFalse();

        verifyNoInteractions(courseRepository);
    }

    @DisplayName("COURSE 모드는 공식 코스로 세션을 생성한다")
    @Test
    void createsCourseRunningSessionWithOfficialCourse() {
        User user = user(2L);
        Course course = course(300L, CourseStatus.OFFICIAL);
        stubUser(user);
        when(courseRepository.findNotDeletedCourse(course.getId())).thenReturn(Optional.of(course));
        when(runningSessionRepository.save(any(RunningSession.class))).thenAnswer(invocation -> persist(invocation.getArgument(0), 902L));

        CreateRunningSessionResponse response = runningSessionService.createRunningSession(
                new CreateRunningSessionRequest(2L, COURSE, 300L, null, STARTED_AT, 30.0, 176.0)
        );

        ArgumentCaptor<RunningSession> sessionCaptor = ArgumentCaptor.forClass(RunningSession.class);
        verify(runningSessionRepository, times(1)).save(sessionCaptor.capture());
        RunningSession savedSession = sessionCaptor.getValue();

        assertThat(savedSession.getCourse()).isEqualTo(course);
        assertThat(savedSession.getGhostTargetRunningSession()).isNull();
        assertThat(savedSession.getMode()).isEqualTo(COURSE);
        assertThat(response.runningSessionId()).isEqualTo(902L);
        assertThat(response.mode()).isEqualTo(COURSE);
        assertThat(response.courseId()).isEqualTo(300L);
        assertThat(response.ghostTargetRunningSessionId()).isNull();
        assertThat(response.distanceM()).isZero();
        assertThat(response.caloriesKcal()).isZero();
        assertThat(response.isCompleted()).isFalse();
    }

    @DisplayName("GHOST 모드는 공식 코스와 완료된 고스트 대상으로 세션을 생성한다")
    @Test
    void createsGhostRunningSessionWithEligibleGhostTarget() {
        User user = user(3L);
        Course course = course(301L, CourseStatus.OFFICIAL);
        RunningSession ghostTarget = completedRunningSession(44L);
        stubUser(user);
        when(courseRepository.findNotDeletedCourse(course.getId())).thenReturn(Optional.of(course));
        when(runningSessionRepository.findByIdAndDeletedAtIsNull(ghostTarget.getId()))
                .thenReturn(Optional.of(ghostTarget));
        when(runningSessionRepository.save(any(RunningSession.class))).thenAnswer(invocation -> persist(invocation.getArgument(0), 903L));

        CreateRunningSessionResponse response = runningSessionService.createRunningSession(
                new CreateRunningSessionRequest(3L, GHOST, 301L, 44L, STARTED_AT, 30.0, 176.0)
        );

        ArgumentCaptor<RunningSession> sessionCaptor = ArgumentCaptor.forClass(RunningSession.class);
        verify(runningSessionRepository, times(1)).save(sessionCaptor.capture());
        RunningSession savedSession = sessionCaptor.getValue();

        assertThat(savedSession.getCourse()).isEqualTo(course);
        assertThat(savedSession.getGhostTargetRunningSession()).isEqualTo(ghostTarget);
        assertThat(savedSession.getMode()).isEqualTo(GHOST);
        assertThat(savedSession.getIsPublic()).isTrue();
        assertThat(response.runningSessionId()).isEqualTo(903L);
        assertThat(response.mode()).isEqualTo(GHOST);
        assertThat(response.courseId()).isEqualTo(301L);
        assertThat(response.ghostTargetRunningSessionId()).isEqualTo(44L);
        assertThat(response.distanceM()).isZero();
        assertThat(response.caloriesKcal()).isZero();
        assertThat(response.isCompleted()).isFalse();
    }

    @DisplayName("없는 사용자는 USER_NOT_FOUND 예외를 던진다")
    @Test
    void throwsUserNotFoundWhenUserDoesNotExist() {
        when(userRepository.findNotDeletedUser(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> runningSessionService.createRunningSession(
                new CreateRunningSessionRequest(10L, FREE, null, null, STARTED_AT, 30.0, 176.0)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.USER_NOT_FOUND);
    }

    @DisplayName("COURSE 모드에서 courseId가 없으면 잘못된 입력 예외를 던진다")
    @Test
    void throwsInvalidInputValueWhenCourseModeHasNoCourseId() {
        stubUser(user(11L));

        assertThatThrownBy(() -> runningSessionService.createRunningSession(
                new CreateRunningSessionRequest(11L, COURSE, null, null, STARTED_AT, 30.0, 176.0)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.INVALID_INPUT_VALUE);

        verify(runningSessionRepository, never()).save(any(RunningSession.class));
    }

    @DisplayName("COURSE 모드에서 공식 코스가 아니면 COURSE_NOT_FOUND 예외를 던진다")
    @Test
    void throwsCourseNotFoundWhenCourseIsNotOfficial() {
        User user = user(12L);
        Course communityCourse = course(302L, CourseStatus.COMMUNITY);
        stubUser(user);
        when(courseRepository.findNotDeletedCourse(communityCourse.getId())).thenReturn(Optional.of(communityCourse));

        assertThatThrownBy(() -> runningSessionService.createRunningSession(
                new CreateRunningSessionRequest(12L, COURSE, 302L, null, STARTED_AT, 30.0, 176.0)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.COURSE_NOT_FOUND);

        verify(runningSessionRepository, never()).save(any(RunningSession.class));
    }

    @DisplayName("GHOST 모드에서 고스트 대상 ID가 없으면 잘못된 입력 예외를 던진다")
    @Test
    void throwsInvalidInputValueWhenGhostModeHasNoGhostTargetId() {
        User user = user(13L);
        Course course = course(303L, CourseStatus.OFFICIAL);
        stubUser(user);
        when(courseRepository.findNotDeletedCourse(course.getId())).thenReturn(Optional.of(course));

        assertThatThrownBy(() -> runningSessionService.createRunningSession(
                new CreateRunningSessionRequest(13L, GHOST, 303L, null, STARTED_AT, 30.0, 176.0)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.INVALID_INPUT_VALUE);

        verify(runningSessionRepository, never()).save(any(RunningSession.class));
    }

    @DisplayName("GHOST 모드에서 고스트 대상을 찾지 못하면 GHOST_TARGET_NOT_FOUND 예외를 던진다")
    @Test
    void throwsGhostTargetNotFoundWhenGhostTargetDoesNotExist() {
        User user = user(14L);
        Course course = course(304L, CourseStatus.OFFICIAL);
        stubUser(user);
        when(courseRepository.findNotDeletedCourse(course.getId())).thenReturn(Optional.of(course));
        when(runningSessionRepository.findByIdAndDeletedAtIsNull(55L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> runningSessionService.createRunningSession(
                new CreateRunningSessionRequest(14L, GHOST, 304L, 55L, STARTED_AT, 30.0, 176.0)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.GHOST_TARGET_NOT_FOUND);

        verify(runningSessionRepository, never()).save(any(RunningSession.class));
    }

    @DisplayName("GHOST 모드에서 완료되지 않은 고스트 대상은 INVALID_GHOST_TARGET 예외를 던진다")
    @Test
    void throwsInvalidGhostTargetWhenGhostTargetIsNotCompleted() {
        User user = user(15L);
        Course course = course(305L, CourseStatus.OFFICIAL);
        RunningSession incompleteGhostTarget = runningSession(56L, false);
        stubUser(user);
        when(courseRepository.findNotDeletedCourse(course.getId())).thenReturn(Optional.of(course));
        when(runningSessionRepository.findByIdAndDeletedAtIsNull(incompleteGhostTarget.getId()))
                .thenReturn(Optional.of(incompleteGhostTarget));

        assertThatThrownBy(() -> runningSessionService.createRunningSession(
                new CreateRunningSessionRequest(15L, GHOST, 305L, 56L, STARTED_AT, 30.0, 176.0)
        ))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.INVALID_GHOST_TARGET);

        verify(runningSessionRepository, never()).save(any(RunningSession.class));
    }

    @DisplayName("종료 요청이 유효하면 세션을 완료 상태로 확정한다")
    @Test
    void completesRunningSessionWhenRequestIsValid() {
        RunningSession runningSession = runningSession(1L, 1L, false, STARTED_AT);
        UpdateRunningSessionRequest request = validUpdateRequest(1L, "2026-03-12T07:12:00");
        when(runningSessionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(runningSession));

        UpdateRunningSessionResponse response = runningSessionService.completeRunningSession(1L, request);

        assertThat(response).isEqualTo(new UpdateRunningSessionResponse(
                1L,
                true,
                false,
                LocalDateTime.parse("2026-03-12T06:30:00"),
                LocalDateTime.parse("2026-03-12T07:12:00"),
                10023,
                642,
                30.0,
                150.0,
                11.0,
                2520,
                2,
                37.5,
                127.0,
                3
        ));
        assertThat(runningSession.getIsCompleted()).isTrue();
        assertThat(runningSession.getEndedAt()).isEqualTo(LocalDateTime.of(2026, 3, 12, 7, 12, 0));
        assertThat(runningSession.getDistanceM()).isEqualTo(10023);
        assertThat(runningSession.getCaloriesKcal()).isEqualTo(642);
        assertThat(runningSession.getEndLatitude()).isEqualTo(30.0);
        assertThat(runningSession.getEndLongitude()).isEqualTo(150.0);
        assertThat(runningSession.getAvgSpeedMps()).isEqualTo(11.0);
        assertThat(runningSession.getDurationSec()).isEqualTo(2520);
        assertThat(runningSession.getAvgPaceSecPerKm()).isEqualTo(2);
        assertThat(runningSession.getElevationGainM()).isEqualTo(3);
    }

    @DisplayName("세션 소유 사용자와 요청 사용자 ID가 다르면 세션 없음으로 처리한다")
    @Test
    void treatsDifferentUserAsRunningSessionNotFound() {
        RunningSession runningSession = runningSession(1L, 2L, false, STARTED_AT);
        UpdateRunningSessionRequest request = validUpdateRequest(1L, "2026-03-12T07:12:00");
        when(runningSessionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(runningSession));

        assertThatThrownBy(() -> runningSessionService.completeRunningSession(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.RUNNING_SESSION_NOT_FOUND);
    }

    @DisplayName("이미 종료된 세션은 중복 종료를 거부한다")
    @Test
    void rejectsAlreadyCompletedRunningSession() {
        RunningSession runningSession = runningSession(1L, 1L, true, STARTED_AT);
        UpdateRunningSessionRequest request = validUpdateRequest(1L, "2026-03-12T07:12:00");
        when(runningSessionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(runningSession));

        assertThatThrownBy(() -> runningSessionService.completeRunningSession(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.RUNNING_SESSION_ALREADY_COMPLETED);
    }

    @DisplayName("존재하지 않는 세션은 종료할 수 없다")
    @Test
    void throwsRunningSessionNotFoundWhenSessionDoesNotExist() {
        UpdateRunningSessionRequest request = validUpdateRequest(1L, "2026-03-12T07:12:00");
        when(runningSessionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> runningSessionService.completeRunningSession(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.RUNNING_SESSION_NOT_FOUND);
    }

    @DisplayName("종료 시각이 시작 시각보다 빨라도 현재 요청 값을 그대로 반영한다")
    @Test
    void appliesEndedAtEvenWhenItIsBeforeStartedAt() {
        RunningSession runningSession = runningSession(1L, 1L, false, STARTED_AT);
        UpdateRunningSessionRequest request = validUpdateRequest(1L, "2026-03-12T06:20:00");
        when(runningSessionRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(runningSession));

        UpdateRunningSessionResponse response = runningSessionService.completeRunningSession(1L, request);

        assertThat(response.endedAt()).isEqualTo(LocalDateTime.of(2026, 3, 12, 6, 20, 0));
        assertThat(runningSession.getEndedAt()).isEqualTo(LocalDateTime.of(2026, 3, 12, 6, 20, 0));
        assertThat(runningSession.getIsCompleted()).isTrue();
    }

    @DisplayName("완료된 러닝 세션을 커뮤니티 코스로 등록한다")
    @Test
    void registersCompletedRunningSessionAsCommunityCourse() {
        RunningSession runningSession = runningSession(81L, 7L, true, STARTED_AT);
        setField(runningSession, "distanceM", 10000);
        setField(runningSession, "elevationGainM", 120);
        setField(runningSession, "snapshotImageUrl", "https://cdn.withrun.app/snapshots/81.png");
        setField(runningSession, "endLatitude", 37.574501);
        setField(runningSession, "endLongitude", 126.989001);
        setField(runningSession, "coordinates", new Coordinates(
                List.of(37.566501, 37.574501),
                List.of(126.978001, 126.989001),
                List.of(0.0, 1.0)
        ));
        RegisterRunningSessionCourseRequest request = new RegisterRunningSessionCourseRequest(
                "한강 야간 러닝 10K",
                "MEDIUM",
                List.of("RIVERSIDE", "URBAN", "RIVERSIDE"),
                "https://cdn.example.com/course/snapshot.png"
        );
        when(runningSessionRepository.findByIdAndDeletedAtIsNull(81L)).thenReturn(Optional.of(runningSession));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> persistCourse(invocation.getArgument(0), 501L));

        RegisterRunningSessionCourseResponse response =
                runningSessionService.registerRunningSessionCourse(81L, request);

        assertThat(response.courseId()).isEqualTo(501L);
        assertThat(response.title()).isEqualTo("한강 야간 러닝 10K");
        assertThat(response.status()).isEqualTo(CourseStatus.COMMUNITY);
        assertThat(response.difficulty()).isEqualTo(Difficulty.MEDIUM);
        assertThat(response.distanceM()).isEqualTo(10000);
        assertThat(response.elevationGainM()).isEqualTo(120);
        assertThat(response.snapshotImageUrl()).isEqualTo("https://cdn.example.com/course/snapshot.png");
        assertThat(response.coordinates()).isEqualTo(new Coordinates(List.of(
                new GeoPoint(37.566501, 126.978001, 0.0),
                new GeoPoint(37.574501, 126.989001, 1.0)
        )));

        ArgumentCaptor<Course> courseCaptor = ArgumentCaptor.forClass(Course.class);
        verify(courseRepository).save(courseCaptor.capture());
        Course savedCourse = courseCaptor.getValue();
        assertThat(savedCourse.getStatus()).isEqualTo(CourseStatus.COMMUNITY);
        assertThat(savedCourse.getUser()).isEqualTo(runningSession.getUser());
        assertThat(savedCourse.getCoordinates()).isEqualTo(runningSession.getCoordinates());
        assertThat(savedCourse.getCoordinates()).isNotSameAs(runningSession.getCoordinates());

        ArgumentCaptor<CourseDifficulty> difficultyCaptor = ArgumentCaptor.forClass(CourseDifficulty.class);
        verify(courseDifficultyRepository).save(difficultyCaptor.capture());
        assertThat(difficultyCaptor.getValue().getDifficulty()).isEqualTo(Difficulty.MEDIUM);
        assertThat(difficultyCaptor.getValue().getCourse()).isEqualTo(savedCourse);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CourseTypeMap>> courseTypeMapsCaptor = ArgumentCaptor.forClass((Class) List.class);
        verify(courseTypeMapRepository).saveAll(courseTypeMapsCaptor.capture());
        List<CourseTypeMap> courseTypeMaps = courseTypeMapsCaptor.getValue();
        assertThat(courseTypeMaps).hasSize(2);
        assertThat(courseTypeMaps)
                .extracting(CourseTypeMap::getCourseType)
                .containsExactly(CourseType.RIVERSIDE, CourseType.URBAN);
        assertThat(courseTypeMaps)
                .extracting(CourseTypeMap::getCourse)
                .containsOnly(savedCourse);
    }

    @DisplayName("요청 이미지 URL이 없으면 러닝 세션 스냅샷을 사용한다")
    @Test
    void fallsBackToRunningSessionSnapshotWhenRequestSnapshotIsMissing() {
        RunningSession runningSession = runningSession(82L, 7L, true, STARTED_AT);
        setField(runningSession, "snapshotImageUrl", "https://cdn.withrun.app/snapshots/82.png");
        setField(runningSession, "coordinates", new Coordinates(
                List.of(37.566501),
                List.of(126.978001),
                List.of(0.0)
        ));
        RegisterRunningSessionCourseRequest request = new RegisterRunningSessionCourseRequest(
                "한강 아침 러닝",
                "EASY",
                List.of("RIVERSIDE"),
                null
        );
        when(runningSessionRepository.findByIdAndDeletedAtIsNull(82L)).thenReturn(Optional.of(runningSession));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> persistCourse(invocation.getArgument(0), 502L));

        RegisterRunningSessionCourseResponse response =
                runningSessionService.registerRunningSessionCourse(82L, request);

        assertThat(response.snapshotImageUrl()).isEqualTo("https://cdn.withrun.app/snapshots/82.png");
    }

    @DisplayName("완료되지 않은 세션은 코스 등록을 거부한다")
    @Test
    void rejectsRegisterCourseWhenRunningSessionIsNotCompleted() {
        RunningSession runningSession = runningSession(83L, 7L, false, STARTED_AT);
        RegisterRunningSessionCourseRequest request = new RegisterRunningSessionCourseRequest(
                "등록 실패 코스",
                "MEDIUM",
                List.of("RIVERSIDE"),
                null
        );
        when(runningSessionRepository.findByIdAndDeletedAtIsNull(83L)).thenReturn(Optional.of(runningSession));

        assertThatThrownBy(() -> runningSessionService.registerRunningSessionCourse(83L, request))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.RUNNING_SESSION_NOT_COMPLETED);

        verify(courseRepository, never()).save(any(Course.class));
        verify(courseDifficultyRepository, never()).save(any(CourseDifficulty.class));
        verify(courseTypeMapRepository, never()).saveAll(any());
    }

    @DisplayName("과거 러닝 세션 페이지를 커서와 시간대로 조립한다")
    @Test
    void returnsPastRunningSessionsWithCursorAndTimeSlot() {
        User user = user(7L);
        when(userRepository.findNotDeletedUser(user.getId())).thenReturn(Optional.of(user));
        when(runningSessionRepository.findPastRunningSessionHistoryRows(
                user.getId(),
                null,
                null,
                null,
                null,
                1
        )).thenReturn(List.of(
                new PastRunningSessionHistoryRow(
                        121L,
                        LocalDateTime.of(2026, 3, 16, 18, 30),
                        7000,
                        2100,
                        420,
                        "https://cdn.withrun.app/snapshots/121.png",
                        40,
                        true,
                        RunningMode.GHOST,
                        GhostResultStatus.WIN
                ),
                new PastRunningSessionHistoryRow(
                        120L,
                        LocalDateTime.of(2026, 3, 15, 5, 45),
                        5000,
                        null,
                        310,
                        null,
                        15,
                        false,
                        RunningMode.FREE,
                        null
                )
        ));

        PastRunningSessionsResponse response = runningSessionService.findPastRunningSessions(
                new PastRunningSessionsRequest(user.getId(), null, null, null, 1, null)
        );

        assertThat(response.items()).containsExactly(
                new PastRunningSessionItemResponse(
                        121L,
                        LocalDateTime.of(2026, 3, 16, 18, 30),
                        7000,
                        2100,
                        420,
                        "https://cdn.withrun.app/snapshots/121.png",
                        40,
                        true,
                        RunningMode.GHOST,
                        GhostResultStatus.WIN,
                        TimeSlot.EVENING
                )
        );
        assertThat(response.hasMore()).isTrue();
        assertThat(response.nextCursor()).isNotBlank();
    }

    @DisplayName("서비스는 별도 객체가 계산한 날짜 범위를 리포지토리에 전달한다")
    @Test
    void passesExtractedDateRangeToRepository() {
        User user = user(7L);
        PastRunningSessionsRequest request = new PastRunningSessionsRequest(user.getId(), 2026, 3, 16, 10, null);
        RunningSessionDateRange range = RunningSessionDateRange.of(request.year(), request.month(), request.day());
        when(userRepository.findNotDeletedUser(user.getId())).thenReturn(Optional.of(user));
        when(runningSessionRepository.findPastRunningSessionHistoryRows(
                user.getId(),
                range.startInclusive(),
                range.endExclusive(),
                null,
                null,
                10
        )).thenReturn(List.of());

        PastRunningSessionsResponse response = runningSessionService.findPastRunningSessions(request);

        assertThat(response.items()).isEmpty();
        assertThat(response.hasMore()).isFalse();
        assertThat(response.nextCursor()).isNull();
    }

    @DisplayName("고스트 러닝 결과 조회 시 결과 상세를 응답한다")
    @Test
    void returnsGhostRunningResultWhenResultExists() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 8, 8, 15);
        GhostRunningResultRow ghostRunningResult = new GhostRunningResultRow(
                77L,
                901L,
                345L,
                202L,
                GhostResultStatus.WIN,
                14,
                32,
                createdAt
        );
        when(ghostRunningResultRepository.findByRunningSessionId(901L))
                .thenReturn(Optional.of(ghostRunningResult));

        GhostRunningResultResponse response = runningSessionService.findGhostRunningResult(901L);

        assertThat(response).isEqualTo(new GhostRunningResultResponse(
                77L,
                901L,
                345L,
                202L,
                GhostResultStatus.WIN,
                14,
                32,
                createdAt
        ));
    }

    @DisplayName("고스트 러닝 결과가 없으면 GHOST_RUNNING_RESULT_NOT_FOUND 예외를 던진다")
    @Test
    void throwsGhostRunningResultNotFoundWhenResultDoesNotExist() {
        when(ghostRunningResultRepository.findByRunningSessionId(901L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> runningSessionService.findGhostRunningResult(901L))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.GHOST_RUNNING_RESULT_NOT_FOUND);
    }

    private void stubUser(User user) {
        when(userRepository.findNotDeletedUser(user.getId())).thenReturn(Optional.of(user));
    }

    private RunningSession persist(RunningSession runningSession, Long id) {
        setField(runningSession, "id", id);
        return runningSession;
    }

    private Course persistCourse(Course course, Long id) {
        setField(course, "id", id);
        return course;
    }

    private Course course(Long id, CourseStatus status) {
        Course course = Course.builder()
                .title("Course " + id)
                .status(status)
                .distanceM(5000)
                .elevationGainM(120)
                .snapshotImageUrl("snapshot-" + id)
                .startLatitude(37.5665)
                .startLongitude(126.9780)
                .endLatitude(37.5700)
                .endLongitude(126.9820)
                .coordinates(new Coordinates(List.of(37.5665), List.of(126.9780), List.of(12.0)))
                .build();
        setField(course, "id", id);
        return course;
    }

    private User user(Long id) {
        User user = instantiate(User.class);
        setField(user, "id", id);
        setField(user, "nickname", "runner-" + id);
        setField(user, "birthDate", LocalDate.of(1995, 3, 11));
        setField(user, "gender", Gender.MALE);
        setField(user, "height", 175.0);
        setField(user, "weight", 68.0);
        return user;
    }

    private RunningSession completedRunningSession(Long id) {
        return runningSession(id, true);
    }

    private RunningSession runningSession(Long id, boolean isCompleted) {
        RunningSession runningSession = instantiate(RunningSession.class);
        setField(runningSession, "id", id);
        setField(runningSession, "isCompleted", isCompleted);
        return runningSession;
    }

    private RunningSession runningSession(Long id, Long userId, boolean isCompleted, LocalDateTime startedAt) {
        RunningSession runningSession = instantiate(RunningSession.class);
        setField(runningSession, "id", id);
        setField(runningSession, "startedAt", startedAt);
        setField(runningSession, "distanceM", 0);
        setField(runningSession, "caloriesKcal", 0);
        setField(runningSession, "elevationGainM", 0);
        setField(runningSession, "startLatitude", 37.5);
        setField(runningSession, "startLongitude", 127.0);
        setField(runningSession, "isPublic", false);
        setField(runningSession, "isCompleted", isCompleted);
        setField(runningSession, "user", user(userId));
        setField(runningSession, "mode", RunningMode.FREE);
        return runningSession;
    }

    private UpdateRunningSessionRequest validUpdateRequest(Long userId, String endedAt) {
        return new UpdateRunningSessionRequest(
                userId,
                LocalDateTime.parse(endedAt),
                10023,
                642,
                30.0,
                150.0,
                11.0,
                2520,
                2,
                3
        );
    }

    private <T> T instantiate(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to instantiate " + type.getSimpleName(), exception);
        }
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
