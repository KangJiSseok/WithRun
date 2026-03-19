package kr.withrun.was.domain.course.repository;

import kr.withrun.was.domain.course.entity.CourseGhostLeaderboard;
import kr.withrun.was.domain.course.repository.query.CourseGhostLeaderboardCustomRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseGhostLeaderboardRepository extends JpaRepository<CourseGhostLeaderboard, Long>, CourseGhostLeaderboardCustomRepository {
    List<CourseGhostLeaderboard> findByUserIdAndCourseIdAndDeletedAtIsNullOrderByPointDescIdAsc(Long userId, Long courseId);
}
