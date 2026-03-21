package kr.withrun.was.domain.course.repository.query;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.entity.QCourse;
import kr.withrun.was.domain.course.entity.QCourseDifficulty;
import kr.withrun.was.domain.course.entity.QCourseTypeMap;
import kr.withrun.was.domain.course.repository.query.dto.NearbyCourseCandidateRow;
import kr.withrun.was.domain.course.type.CourseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class CourseCustomRepositoryImpl implements CourseCustomRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<Course> findNotDeletedCourse(Long courseId) {
        QCourse course = QCourse.course;

        return Optional.ofNullable(
                queryFactory
                        .selectFrom(course)
                        .where(
                                course.id.eq(courseId),
                                course.deletedAt.isNull()
                        )
                        .fetchOne()
        );
    }

    @Override
    public List<NearbyCourseCandidateRow> findOfficialNearbyCourseCandidates() {
        QCourse course = QCourse.course;
        QCourseDifficulty difficulty = QCourseDifficulty.courseDifficulty;
        QCourseTypeMap courseTypeMap = QCourseTypeMap.courseTypeMap;

        return queryFactory
                .select(Projections.constructor(
                        NearbyCourseCandidateRow.class,
                        course.id,
                        course.title,
                        course.status,
                        course.distanceM,
                        course.elevationGainM,
                        course.startLatitude,
                        course.startLongitude,
                        course.endLatitude,
                        course.endLongitude,
                        course.snapshotImageUrl,
                        course.coordinates,
                        difficulty.difficulty,
                        courseTypeMap.courseType
                ))
                .from(course)
                .leftJoin(difficulty).on(difficulty.course.eq(course)
                        .and(difficulty.deletedAt.isNull()))
                .leftJoin(courseTypeMap).on(courseTypeMap.course.eq(course)
                        .and(courseTypeMap.deletedAt.isNull()))
                .where(course.status.eq(CourseStatus.OFFICIAL)
                        .and(course.deletedAt.isNull())
                        .and(course.coordinates.isNotNull()))
                .fetch();
    }

    @Override
    public List<NearbyCourseCandidateRow> findOfficialNearbyCourseCandidates(List<Integer> preferredDistanceMs) {
        // 오차 범위 상수값으로 처리
        final int TARGET_DISTANCE_TOLERANCE_M = 200;

        QCourse course = QCourse.course;
        QCourseDifficulty difficulty = QCourseDifficulty.courseDifficulty;
        QCourseTypeMap courseTypeMap = QCourseTypeMap.courseTypeMap;

        BooleanBuilder preferredDistancePredicate = new BooleanBuilder();
        for (Integer preferredDistanceM : preferredDistanceMs) {
            int minDistanceM = Math.max(0, preferredDistanceM - TARGET_DISTANCE_TOLERANCE_M);
            int maxDistanceM = preferredDistanceM + TARGET_DISTANCE_TOLERANCE_M;

            preferredDistancePredicate.or(course.distanceM.between(minDistanceM, maxDistanceM));
        }

        BooleanBuilder whereClause = new BooleanBuilder()
                .and(course.status.eq(CourseStatus.OFFICIAL))
                .and(course.deletedAt.isNull())
                .and(course.coordinates.isNotNull());


        whereClause.and(preferredDistancePredicate);

        return queryFactory
                .select(Projections.constructor(
                        NearbyCourseCandidateRow.class,
                        course.id,
                        course.title,
                        course.status,
                        course.distanceM,
                        course.elevationGainM,
                        course.startLatitude,
                        course.startLongitude,
                        course.endLatitude,
                        course.endLongitude,
                        course.snapshotImageUrl,
                        course.coordinates,
                        difficulty.difficulty,
                        courseTypeMap.courseType
                ))
                .from(course)
                .leftJoin(difficulty).on(
                        difficulty.course.eq(course)
                                .and(difficulty.deletedAt.isNull())
                )
                .leftJoin(courseTypeMap).on(
                        courseTypeMap.course.eq(course)
                                .and(courseTypeMap.deletedAt.isNull())
                )
                .where(whereClause)
                .fetch();
    }
}
