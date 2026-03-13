package kr.withrun.was.domain.running.service;

import kr.withrun.was.domain.running.dto.UpdateRunningSessionRequest;
import kr.withrun.was.domain.running.dto.UpdateRunningSessionResponse;
import kr.withrun.was.domain.running.entity.RunningSession;
import kr.withrun.was.domain.running.repository.RunningSessionRepository;
import kr.withrun.was.domain.running.type.RunningMode;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("러닝 세션 서비스")
class RunningSessionServiceTest {

    @Mock
    private RunningSessionRepository runningSessionRepository;

    private RunningSessionService runningSessionService;

    @BeforeEach
    void setUp() {
        runningSessionService = new RunningSessionService(runningSessionRepository);
    }

    @DisplayName("서비스는 클래스 기본 readOnly와 쓰기 메서드 트랜잭션을 함께 사용한다")
    @Test
    void usesReadOnlyAtClassLevelAndWritableMethods() throws NoSuchMethodException {
        Transactional classTransactional = RunningSessionService.class.getAnnotation(Transactional.class);
        Transactional completeTransactional = RunningSessionService.class
                .getDeclaredMethod("completeRunningSession", Long.class, UpdateRunningSessionRequest.class)
                .getAnnotation(Transactional.class);

        assertThat(classTransactional).isNotNull();
        assertThat(classTransactional.readOnly()).isTrue();
        assertThat(completeTransactional).isNotNull();
        assertThat(completeTransactional.readOnly()).isFalse();
    }

    @DisplayName("종료 요청이 유효하면 세션을 완료 상태로 확정한다")
    @Test
    void completesRunningSessionWhenRequestIsValid() {
        RunningSession runningSession = runningSession(1L, 1L, false, LocalDateTime.of(2026, 3, 12, 6, 30, 0));
        UpdateRunningSessionRequest request = validRequest(1L, "2026-03-12T07:12:00");
        given(runningSessionRepository.findNotDeletedRunningSession(1L)).willReturn(Optional.of(runningSession));

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
        RunningSession runningSession = runningSession(1L, 2L, false, LocalDateTime.of(2026, 3, 12, 6, 30, 0));
        UpdateRunningSessionRequest request = validRequest(1L, "2026-03-12T07:12:00");
        given(runningSessionRepository.findNotDeletedRunningSession(1L)).willReturn(Optional.of(runningSession));

        assertThatThrownBy(() -> runningSessionService.completeRunningSession(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.RUNNING_SESSION_NOT_FOUND);
    }

    @DisplayName("이미 종료된 세션은 중복 종료를 거부한다")
    @Test
    void rejectsAlreadyCompletedRunningSession() {
        RunningSession runningSession = runningSession(1L, 1L, true, LocalDateTime.of(2026, 3, 12, 6, 30, 0));
        UpdateRunningSessionRequest request = validRequest(1L, "2026-03-12T07:12:00");
        given(runningSessionRepository.findNotDeletedRunningSession(1L)).willReturn(Optional.of(runningSession));

        assertThatThrownBy(() -> runningSessionService.completeRunningSession(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.RUNNING_SESSION_ALREADY_COMPLETED);
    }

    @DisplayName("존재하지 않는 세션은 종료할 수 없다")
    @Test
    void throwsRunningSessionNotFoundWhenSessionDoesNotExist() {
        UpdateRunningSessionRequest request = validRequest(1L, "2026-03-12T07:12:00");
        given(runningSessionRepository.findNotDeletedRunningSession(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> runningSessionService.completeRunningSession(1L, request))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.RUNNING_SESSION_NOT_FOUND);
    }

    @DisplayName("종료 시각이 시작 시각보다 빨라도 현재 요청 값을 그대로 반영한다")
    @Test
    void appliesEndedAtEvenWhenItIsBeforeStartedAt() {
        RunningSession runningSession = runningSession(1L, 1L, false, LocalDateTime.of(2026, 3, 12, 6, 30, 0));
        UpdateRunningSessionRequest request = validRequest(1L, "2026-03-12T06:20:00");
        given(runningSessionRepository.findNotDeletedRunningSession(1L)).willReturn(Optional.of(runningSession));

        UpdateRunningSessionResponse response = runningSessionService.completeRunningSession(1L, request);

        assertThat(response.endedAt()).isEqualTo(LocalDateTime.of(2026, 3, 12, 6, 20, 0));
        assertThat(runningSession.getEndedAt()).isEqualTo(LocalDateTime.of(2026, 3, 12, 6, 20, 0));
        assertThat(runningSession.getIsCompleted()).isTrue();
    }

    private RunningSession runningSession(Long id, Long userId, boolean isCompleted, LocalDateTime startedAt) {
        RunningSession runningSession = instantiateRunningSession();
        User user = org.mockito.Mockito.mock(User.class);
        given(user.getId()).willReturn(userId);

        ReflectionTestUtils.setField(runningSession, "id", id);
        ReflectionTestUtils.setField(runningSession, "startedAt", startedAt);
        ReflectionTestUtils.setField(runningSession, "distanceM", 0);
        ReflectionTestUtils.setField(runningSession, "caloriesKcal", 0);
        ReflectionTestUtils.setField(runningSession, "elevationGainM", 0);
        ReflectionTestUtils.setField(runningSession, "startLatitude", 37.5);
        ReflectionTestUtils.setField(runningSession, "startLongitude", 127.0);
        ReflectionTestUtils.setField(runningSession, "isPublic", false);
        ReflectionTestUtils.setField(runningSession, "isCompleted", isCompleted);
        ReflectionTestUtils.setField(runningSession, "user", user);
        ReflectionTestUtils.setField(runningSession, "mode", RunningMode.FREE);
        return runningSession;
    }

    private RunningSession instantiateRunningSession() {
        try {
            var constructor = RunningSession.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private UpdateRunningSessionRequest validRequest(Long userId, String endedAt) {
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
}
