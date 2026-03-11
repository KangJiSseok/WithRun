package kr.withrun.was.domain.course.repository.query;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import kr.withrun.was.domain.course.entity.QCourse;
import kr.withrun.was.domain.course.entity.QCourseDifficulty;
import kr.withrun.was.domain.course.entity.QCourseTypeMap;
import kr.withrun.was.domain.course.repository.query.dto.NearbyCourseCandidateRow;
import kr.withrun.was.domain.course.type.CourseStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class CourseQueryRepositoryImpl implements CourseQueryRepository {

    private final JPAQueryFactory queryFactory;

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
                        .and(course.deletedAt.isNull()))
                .fetch();
    }
}
