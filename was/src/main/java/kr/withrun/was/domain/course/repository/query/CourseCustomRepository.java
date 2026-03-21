package kr.withrun.was.domain.course.repository.query;

import kr.withrun.was.domain.course.dto.PreferredDistanceRange;
import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.repository.query.dto.NearbyCourseCandidateRow;

import java.util.List;
import java.util.Optional;

public interface CourseCustomRepository {

    Optional<Course> findNotDeletedCourse(Long courseId);

    List<NearbyCourseCandidateRow> findOfficialNearbyCourseCandidates();

    List<NearbyCourseCandidateRow> findOfficialNearbyCourseCandidates(List<PreferredDistanceRange> preferredDistanceMs);
}
