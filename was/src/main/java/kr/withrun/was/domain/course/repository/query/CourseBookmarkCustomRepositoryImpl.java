package kr.withrun.was.domain.course.repository.query;

import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.withrun.was.domain.course.entity.CourseBookmark;
import kr.withrun.was.domain.course.entity.QCourseBookmark;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CourseBookmarkCustomRepositoryImpl implements CourseBookmarkCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<CourseBookmark> findBookmark(Long courseId, Long userId) {
        QCourseBookmark courseBookmark = QCourseBookmark.courseBookmark;

        return Optional.ofNullable(
                queryFactory
                        .selectFrom(courseBookmark)
                        .where(
                                courseBookmark.course.id.eq(courseId),
                                courseBookmark.user.id.eq(userId)
                        )
                        .fetchOne()
        );
    }

    @Override
    public void deleteBookmark(Long courseId, Long userId) {
        QCourseBookmark courseBookmark = QCourseBookmark.courseBookmark;

        queryFactory
                .delete(courseBookmark)
                .where(
                        courseBookmark.course.id.eq(courseId),
                        courseBookmark.user.id.eq(userId)
                )
                .execute();
    }
}
