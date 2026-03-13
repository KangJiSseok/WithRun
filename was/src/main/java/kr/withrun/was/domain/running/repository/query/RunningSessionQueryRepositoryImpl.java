package kr.withrun.was.domain.running.repository.query;

import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.withrun.was.domain.running.entity.QRunningSession;
import kr.withrun.was.domain.running.entity.RunningSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RunningSessionQueryRepositoryImpl implements RunningSessionQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<RunningSession> findNotDeletedRunningSession(Long runningSessionId) {
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
}
