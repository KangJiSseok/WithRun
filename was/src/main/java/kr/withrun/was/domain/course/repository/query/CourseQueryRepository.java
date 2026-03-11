package kr.withrun.was.domain.course.repository.query;

import kr.withrun.was.domain.course.repository.query.dto.NearbyCourseCandidateRow;

import java.util.List;

public interface CourseQueryRepository {
    
    List<NearbyCourseCandidateRow> findOfficialNearbyCourseCandidates();
}
