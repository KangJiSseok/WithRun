package kr.withrun.was.domain.course.repository;

public interface CourseLikeQueryRepository {

    long countActiveLikedByCourseId(Long courseId);
}
