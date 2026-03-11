package kr.withrun.was.domain.course.repository.query;

import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.withrun.was.domain.course.entity.QCourseLike;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CourseLikeCustomRepositoryImpl implements CourseLikeCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public long countByCourseId(Long courseId) {
        QCourseLike courseLike = QCourseLike.courseLike;

        Long result = queryFactory
                .select(courseLike.count())
                .from(courseLike)
                .where(courseLike.course.id.eq(courseId))
                .fetchOne();

        return result == null ? 0L : result;
    }
}
