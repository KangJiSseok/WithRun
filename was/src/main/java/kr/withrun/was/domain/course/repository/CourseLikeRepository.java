package kr.withrun.was.domain.course.repository;

import kr.withrun.was.domain.course.entity.CourseLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourseLikeRepository extends JpaRepository<CourseLike, Long>, CourseLikeQueryRepository {

    Optional<CourseLike> findByCourseIdAndUserId(Long courseId, Long userId);
}
