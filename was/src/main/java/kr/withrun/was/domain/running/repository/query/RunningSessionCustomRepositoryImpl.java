package kr.withrun.was.domain.running.repository.query;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.withrun.was.domain.running.entity.QGhostRunningResult;
import kr.withrun.was.domain.running.entity.QRunningSession;
import kr.withrun.was.domain.running.entity.RunningSession;
import kr.withrun.was.domain.running.repository.query.dto.RunningSessionHistoryRow;
import kr.withrun.was.domain.user.entity.QUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RunningSessionCustomRepositoryImpl implements RunningSessionCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<RunningSession> findByIdAndDeletedAtIsNull(Long runningSessionId) {
        QRunningSession runningSession = QRunningSession.runningSession;

        return Optional.ofNullable(
                queryFactory
                        .selectFrom(runningSession)
                        .where(
                                runningSession.id.eq(runningSessionId),
                                runningSession.deletedAt.isNull()
                        )
                        .fetchOne()
        );
    }

    @Override
    public List<RunningSessionHistoryRow> findRunningSessionHistoryRows(
            Long userId,
            LocalDateTime cursorStartedAt,
            Long cursorRunningSessionId,
            int pageSize
    ) {
        QRunningSession runningSession = QRunningSession.runningSession;
        QGhostRunningResult ghostRunningResult = QGhostRunningResult.ghostRunningResult;
        QUser user = QUser.user;

        return queryFactory
                .select(Projections.constructor(
                        RunningSessionHistoryRow.class,
                        runningSession.id,
                        runningSession.startedAt,
                        runningSession.distanceM,
                        runningSession.durationSec,
                        runningSession.caloriesKcal,
                        runningSession.snapshotImageUrl,
                        runningSession.elevationGainM,
                        runningSession.isCompleted,
                        runningSession.mode,
                        ghostRunningResult.resultStatus
                ))
                .from(runningSession)
                .join(runningSession.user, user)
                .leftJoin(ghostRunningResult).on(
                        ghostRunningResult.runningSession.eq(runningSession)
                                .and(ghostRunningResult.deletedAt.isNull())
                )
                .where(
                        user.id.eq(userId),
                        user.deletedAt.isNull(),
                        runningSession.deletedAt.isNull(),
                        isAfterCursor(runningSession, cursorStartedAt, cursorRunningSessionId)
                )
                .orderBy(
                        runningSession.startedAt.desc(),
                        runningSession.id.desc()
                )
                .limit((long) pageSize + 1)
                .fetch();
    }

    private BooleanExpression isAfterCursor(
            QRunningSession runningSession,
            LocalDateTime cursorStartedAt,
            Long cursorRunningSessionId
    ) {
        if (cursorStartedAt == null || cursorRunningSessionId == null) {
            return null;
        }

        return runningSession.startedAt.lt(cursorStartedAt)
                .or(runningSession.startedAt.eq(cursorStartedAt)
                        .and(runningSession.id.lt(cursorRunningSessionId)));
    }

}
