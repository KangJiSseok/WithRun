package kr.withrun.was.domain.running.repository;

import jakarta.persistence.EntityManager;
import kr.withrun.was.domain.running.entity.GhostRunningResult;
import kr.withrun.was.domain.running.entity.RunningSession;
import kr.withrun.was.domain.running.repository.query.RunningSessionCustomRepository;
import kr.withrun.was.domain.running.repository.query.dto.RunningSessionHistoryRow;
import kr.withrun.was.domain.running.type.GhostResultStatus;
import kr.withrun.was.domain.running.type.RunningMode;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.type.Gender;
import kr.withrun.was.global.config.JpaAuditingConfig;
import kr.withrun.was.global.config.QuerydslConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.config.import=",
        "spring.cloud.aws.parameterstore.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:running-session-repository;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("러닝 세션 리포지토리")
class RunningSessionRepositoryTest {

    @Autowired
    private RunningSessionRepository runningSessionRepository;

    @Autowired
    private EntityManager entityManager;

    @DisplayName("러닝 세션 히스토리 row를 시작 시각 역순과 세션 ID 역순으로 조회하고 삭제 데이터는 제외한다")
    @Test
    void findsRunningSessionHistoryRowsOrderedAndFiltered() {
        assertThat(runningSessionRepository).isInstanceOf(RunningSessionCustomRepository.class);

        User user = persistUser("history-runner");
        User anotherUser = persistUser("another-runner");
        LocalDateTime sharedStartedAt = LocalDateTime.of(2026, 3, 15, 19, 30);

        RunningSession sharedLowerId = persistSession(user, RunningMode.FREE, sharedStartedAt, true, 5000, 1800, 300, 18, "snapshot-1");
        RunningSession sharedHigherId = persistSession(user, RunningMode.GHOST, sharedStartedAt, true, 7200, 2100, 430, 42, "snapshot-2");
        RunningSession olderSession = persistSession(user, RunningMode.COURSE, LocalDateTime.of(2026, 3, 15, 6, 30), false, 3200, null, 180, 10, null);
        RunningSession deletedSession = persistSession(user, RunningMode.FREE, LocalDateTime.of(2026, 3, 16, 8, 0), true, 9000, 2500, 500, 55, "snapshot-deleted");
        deletedSession.delete();
        persistSession(anotherUser, RunningMode.FREE, LocalDateTime.of(2026, 3, 16, 9, 0), true, 8800, 2400, 480, 40, "snapshot-other");
        persistGhostResult(sharedHigherId, GhostResultStatus.WIN);

        entityManager.flush();
        entityManager.clear();

        List<RunningSessionHistoryRow> rows = runningSessionRepository.findRunningSessionHistoryRows(user.getId(), null, null, 10);

        assertThat(rows).hasSize(3);
        assertThat(rows)
                .extracting(RunningSessionHistoryRow::runningSessionId)
                .containsExactly(sharedHigherId.getId(), sharedLowerId.getId(), olderSession.getId());
        assertThat(rows)
                .extracting(RunningSessionHistoryRow::runningMode)
                .containsExactly(RunningMode.GHOST, RunningMode.FREE, RunningMode.COURSE);
        assertThat(rows)
                .extracting(RunningSessionHistoryRow::ghostResultStatus)
                .containsExactly(GhostResultStatus.WIN, null, null);
    }

    @DisplayName("러닝 세션 히스토리 row는 page size + 1개까지만 조회하고 커서를 적용한다")
    @Test
    void limitsRunningSessionHistoryRowsAndAppliesCursor() {
        User user = persistUser("cursor-runner");
        LocalDateTime newestStartedAt = LocalDateTime.of(2026, 3, 16, 7, 0);
        LocalDateTime sharedStartedAt = LocalDateTime.of(2026, 3, 15, 19, 30);

        RunningSession newest = persistSession(user, RunningMode.FREE, newestStartedAt, true, 9000, 2600, 520, 58, "snapshot-newest");
        RunningSession sharedLowerId = persistSession(user, RunningMode.COURSE, sharedStartedAt, true, 6500, 1900, 380, 24, "snapshot-middle-1");
        RunningSession sharedHigherId = persistSession(user, RunningMode.GHOST, sharedStartedAt, true, 7000, 2000, 410, 31, "snapshot-middle-2");
        RunningSession oldest = persistSession(user, RunningMode.FREE, LocalDateTime.of(2026, 3, 15, 6, 0), false, 2800, null, 150, 8, null);

        entityManager.flush();
        entityManager.clear();

        List<RunningSessionHistoryRow> firstPageRows = runningSessionRepository.findRunningSessionHistoryRows(user.getId(), null, null, 2);

        assertThat(firstPageRows).hasSize(3);
        assertThat(firstPageRows)
                .extracting(RunningSessionHistoryRow::runningSessionId)
                .containsExactly(newest.getId(), sharedHigherId.getId(), sharedLowerId.getId());
        assertThat(firstPageRows)
                .extracting(RunningSessionHistoryRow::runningSessionId)
                .doesNotContain(oldest.getId());

        List<RunningSessionHistoryRow> cursorRows = runningSessionRepository.findRunningSessionHistoryRows(
                user.getId(),
                sharedStartedAt,
                sharedHigherId.getId(),
                10
        );

        assertThat(cursorRows)
                .extracting(RunningSessionHistoryRow::runningSessionId)
                .containsExactly(sharedLowerId.getId(), oldest.getId());
    }

    @DisplayName("삭제된 고스트 결과는 러닝 세션 히스토리에서 제외한다")
    @Test
    void excludesDeletedGhostRunningResultFromHistory() {
        User user = persistUser("ghost-result-runner");
        RunningSession ghostSession = persistSession(
                user,
                RunningMode.GHOST,
                LocalDateTime.of(2026, 3, 15, 19, 30),
                true,
                7200,
                2100,
                430,
                42,
                "snapshot-ghost"
        );
        GhostRunningResult deletedGhostResult = persistGhostResult(ghostSession, GhostResultStatus.WIN);
        deletedGhostResult.delete();

        entityManager.flush();
        entityManager.clear();

        List<RunningSessionHistoryRow> rows = runningSessionRepository.findRunningSessionHistoryRows(user.getId(), null, null, 10);

        assertThat(rows).hasSize(1);
        assertThat(rows.getFirst().ghostResultStatus()).isNull();
    }

    private User persistUser(String nickname) {
        User user = instantiate(User.class);
        setField(user, "nickname", nickname);
        setField(user, "birthDate", LocalDate.of(1995, 3, 11));
        setField(user, "gender", Gender.MALE);
        setField(user, "height", 175.0);
        setField(user, "weight", 68.0);
        entityManager.persist(user);
        return user;
    }

    private RunningSession persistSession(
            User user,
            RunningMode mode,
            LocalDateTime startedAt,
            boolean isCompleted,
            int distanceM,
            Integer durationSec,
            int caloriesKcal,
            int elevationGainM,
            String snapshotImageUrl
    ) {
        RunningSession runningSession = RunningSession.start(
                user,
                mode,
                null,
                null,
                startedAt,
                37.5665,
                126.9780
        );
        if (isCompleted) {
            runningSession.complete(
                    startedAt.plusSeconds(durationSec != null ? durationSec : 1),
                    distanceM,
                    caloriesKcal,
                    37.57,
                    126.98,
                    3.5,
                    durationSec != null ? durationSec : 1,
                    300,
                    elevationGainM
            );
        }
        setField(runningSession, "distanceM", distanceM);
        setField(runningSession, "durationSec", durationSec);
        setField(runningSession, "caloriesKcal", caloriesKcal);
        setField(runningSession, "elevationGainM", elevationGainM);
        setField(runningSession, "snapshotImageUrl", snapshotImageUrl);
        entityManager.persist(runningSession);
        return runningSession;
    }

    private GhostRunningResult persistGhostResult(RunningSession runningSession, GhostResultStatus status) {
        GhostRunningResult ghostRunningResult = instantiate(GhostRunningResult.class);
        setField(ghostRunningResult, "runningSession", runningSession);
        setField(ghostRunningResult, "ghostTargetRunningSession", runningSession);
        setField(ghostRunningResult, "targetUser", runningSession.getUser());
        setField(ghostRunningResult, "resultStatus", status);
        setField(ghostRunningResult, "timeGapSec", 12);
        setField(ghostRunningResult, "distanceGapM", 25);
        entityManager.persist(ghostRunningResult);
        return ghostRunningResult;
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
