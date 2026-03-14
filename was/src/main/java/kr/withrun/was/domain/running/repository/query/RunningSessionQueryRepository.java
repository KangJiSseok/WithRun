package kr.withrun.was.domain.running.repository.query;

import kr.withrun.was.domain.running.entity.RunningSession;

import java.util.Optional;

public interface RunningSessionQueryRepository {

    Optional<RunningSession> findNotDeletedRunningSession(Long runningSessionId);
}
