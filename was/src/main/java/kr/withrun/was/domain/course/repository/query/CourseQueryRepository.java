package kr.withrun.was.domain.course.repository.query;

import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.repository.query.dto.NearbyCourseCandidateRow;

import java.util.Optional;
import java.util.List;

public interface CourseQueryRepository {

    Optional<Course> findNotDeletedCourse(Long courseId);

    List<NearbyCourseCandidateRow> findOfficialNearbyCourseCandidates();
}
