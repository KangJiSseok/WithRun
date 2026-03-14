package kr.withrun.was.domain.course.repository.query;

import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.withrun.was.domain.course.entity.CourseBookmark;
import kr.withrun.was.domain.course.entity.QCourse;
import kr.withrun.was.domain.course.entity.QCourseBookmark;
import kr.withrun.was.domain.course.entity.QCourseDifficulty;
import kr.withrun.was.domain.course.entity.QCourseTypeMap;
import kr.withrun.was.domain.course.repository.query.dto.BookmarkedCourseRow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
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
    public List<BookmarkedCourseRow> findBookmarkedCourseRows(Long userId, LocalDateTime cursorBookmarkedAt, Long cursorBookmarkId, int pageSize) {
        QCourseBookmark courseBookmark = QCourseBookmark.courseBookmark;
        QCourse course = QCourse.course;
        QCourseDifficulty courseDifficulty = QCourseDifficulty.courseDifficulty;
        QCourseTypeMap courseTypeMap = QCourseTypeMap.courseTypeMap;

        List<Long> pagedBookmarkIds = queryFactory
                .select(courseBookmark.id)
                .from(courseBookmark)
                .join(courseBookmark.course, course)
                .where(
                        courseBookmark.user.id.eq(userId),
                        course.deletedAt.isNull(),
                        isAfterCursor(courseBookmark, cursorBookmarkedAt, cursorBookmarkId)
                )
                .orderBy(
                        courseBookmark.createdAt.desc(),
                        courseBookmark.id.desc()
                )
                .limit((long) pageSize + 1)
                .fetch();

        if (pagedBookmarkIds.isEmpty()) {
            return List.of();
        }

        return queryFactory
                .select(
                        courseBookmark.id,
                        courseBookmark.createdAt,
                        course.id,
                        course.title,
                        course.status,
                        course.distanceM,
                        course.elevationGainM,
                        courseDifficulty.difficulty,
                        courseTypeMap.courseType,
                        course.snapshotImageUrl,
                        Expressions.constant(Boolean.TRUE)
                )
                .from(courseBookmark)
                .join(courseBookmark.course, course)
                .leftJoin(courseDifficulty).on(courseDifficulty.course.eq(course)
                        .and(courseDifficulty.deletedAt.isNull()))
                .leftJoin(courseTypeMap).on(courseTypeMap.course.eq(course)
                        .and(courseTypeMap.deletedAt.isNull()))
                .where(
                        courseBookmark.id.in(pagedBookmarkIds),
                        courseBookmark.user.id.eq(userId),
                        course.deletedAt.isNull()
                )
                .orderBy(
                        courseBookmark.createdAt.desc(),
                        courseBookmark.id.desc(),
                        courseTypeMap.id.asc().nullsLast()
                )
                .fetch()
                .stream()
                .map(tuple -> toBookmarkedCourseRow(tuple, courseBookmark, course, courseDifficulty, courseTypeMap))
                .toList();
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

    private BookmarkedCourseRow toBookmarkedCourseRow(
            Tuple tuple,
            QCourseBookmark courseBookmark,
            QCourse course,
            QCourseDifficulty courseDifficulty,
            QCourseTypeMap courseTypeMap
    ) {
        return new BookmarkedCourseRow(
                tuple.get(courseBookmark.id),
                tuple.get(courseBookmark.createdAt),
                tuple.get(course.id),
                tuple.get(course.title),
                tuple.get(course.status),
                tuple.get(course.distanceM),
                tuple.get(course.elevationGainM),
                tuple.get(courseDifficulty.difficulty),
                tuple.get(courseTypeMap.courseType),
                tuple.get(course.snapshotImageUrl),
                tuple.get(Expressions.constant(Boolean.TRUE))
        );
    }

    private BooleanExpression isAfterCursor(
            QCourseBookmark courseBookmark,
            LocalDateTime cursorBookmarkedAt,
            Long cursorBookmarkId
    ) {
        if (cursorBookmarkedAt == null || cursorBookmarkId == null) {
            return null;
        }

        return courseBookmark.createdAt.lt(cursorBookmarkedAt)
                .or(courseBookmark.createdAt.eq(cursorBookmarkedAt)
                        .and(courseBookmark.id.lt(cursorBookmarkId)));
    }
}
