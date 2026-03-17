package kr.withrun.was.domain.running.repository;

import kr.withrun.was.domain.running.entity.RunningHealthSample;
import kr.withrun.was.domain.running.repository.query.RunningHealthSampleCustomRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RunningHealthSampleRepository extends JpaRepository<RunningHealthSample, Long>, RunningHealthSampleCustomRepository {
}
