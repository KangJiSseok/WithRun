package kr.withrun.was.domain.running.repository.query;

import kr.withrun.was.domain.running.entity.RunningSession;
import kr.withrun.was.domain.running.repository.query.dto.RunningSessionHistoryRow;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RunningSessionCustomRepository {

    Optional<RunningSession> findByIdAndDeletedAtIsNull(Long id);

    List<RunningSessionHistoryRow> findRunningSessionHistoryRows(
            Long userId,
            LocalDateTime cursorStartedAt,
            Long cursorRunningSessionId,
            int pageSize
    );

}
