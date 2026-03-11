package kr.withrun.was.domain.course.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.withrun.was.domain.course.entity.QCourseLike;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CourseLikeQueryRepositoryImpl implements CourseLikeQueryRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public long countActiveLikedByCourseId(Long courseId) {
        QCourseLike courseLike = QCourseLike.courseLike;

        Long result = queryFactory
                .select(courseLike.count())
                .from(courseLike)
                .where(courseLike.course.id.eq(courseId)
                        .and(courseLike.isLiked.isTrue()))
                .fetchOne();

        return result == null ? 0L : result;
    }
}
