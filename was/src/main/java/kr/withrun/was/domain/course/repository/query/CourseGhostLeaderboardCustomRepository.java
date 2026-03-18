package kr.withrun.was.domain.course.repository.query;

import kr.withrun.was.domain.course.repository.query.dto.CourseGhostLeaderboardRankRow;

import java.util.List;

public interface CourseGhostLeaderboardCustomRepository {

    List<CourseGhostLeaderboardRankRow> findRankedRowsByCourseId(Long courseId, int page, int size);

    long countByCourseId(Long courseId);

}
