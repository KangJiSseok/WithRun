package kr.withrun.was.domain.course.repository.query;

import kr.withrun.was.domain.course.entity.CourseBookmark;

import java.util.Optional;

public interface CourseBookmarkCustomRepository {

    Optional<CourseBookmark> findBookmark(Long courseId, Long userId);

    void deleteBookmark(Long courseId, Long userId);
}
