package kr.withrun.was.domain.course.repository;

import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.repository.query.CourseQueryRepository;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CourseRepository extends JpaRepository<Course, Long>, CourseQueryRepository {

}
