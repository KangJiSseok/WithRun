package kr.withrun.was.domain.running.repository;

import kr.withrun.was.domain.running.entity.RunningSession;
import kr.withrun.was.domain.running.repository.query.RunningSessionQueryRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RunningSessionRepository extends JpaRepository<RunningSession, Long>, RunningSessionQueryRepository {
}
