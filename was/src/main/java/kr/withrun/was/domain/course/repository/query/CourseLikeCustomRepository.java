package kr.withrun.was.domain.course.repository.query;

public interface CourseLikeCustomRepository {

    long countActiveLikedByCourseId(Long courseId);
}
