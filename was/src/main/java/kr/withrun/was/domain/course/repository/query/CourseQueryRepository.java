package kr.withrun.was.domain.course.repository.query;

import kr.withrun.was.domain.course.repository.query.dto.NearbyCourseCandidateRow;

import java.util.Optional;
import java.util.List;
import kr.withrun.was.domain.course.entity.Course;

public interface CourseQueryRepository {

    Optional<Course> findActiveById(Long courseId);

    List<NearbyCourseCandidateRow> findOfficialNearbyCourseCandidates();
}
