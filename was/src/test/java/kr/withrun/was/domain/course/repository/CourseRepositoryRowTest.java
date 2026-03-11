package kr.withrun.was.domain.course.repository;

import jakarta.persistence.EntityManager;
import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.entity.CourseDifficulty;
import kr.withrun.was.domain.course.entity.CourseTypeMap;
import kr.withrun.was.domain.course.repository.query.dto.NearbyCourseCandidateRow;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.global.common.type.Difficulty;
import kr.withrun.was.global.config.QuerydslConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.config.import=",
        "spring.cloud.aws.parameterstore.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:course-row;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import(QuerydslConfig.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("코스 리포지토리 Row")
class CourseRepositoryRowTest {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EntityManager entityManager;

    @DisplayName("좌표 정보가 저장과 Row 조회를 거쳐도 유지된다")
    @Test
    void preservesCoordinatesThroughPersistenceAndRowQuery() {
        Coordinates coordinates = new Coordinates(
                List.of(37.5665, 37.5668),
                List.of(126.9780, 126.9791),
                List.of(12.0, 15.5)
        );
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 11, 1, 0);

        Course course = Course.builder()
                .title("Row Course")
                .status(CourseStatus.OFFICIAL)
                .distanceM(10000)
                .elevationGainM(40)
                .snapshotImageUrl("snapshot")
                .startLatitude(37.5665)
                .startLongitude(126.9780)
                .endLatitude(37.5700)
                .endLongitude(126.9820)
                .coordinates(coordinates)
                .build();
        setCreatedAt(course, createdAt);
        course = courseRepository.save(course);

        CourseDifficulty courseDifficulty = CourseDifficulty.create(course, Difficulty.MEDIUM);
        CourseTypeMap courseTypeMap = CourseTypeMap.create(course, CourseType.PARK);
        setCreatedAt(courseDifficulty, createdAt);
        setCreatedAt(courseTypeMap, createdAt);

        entityManager.persist(courseDifficulty);
        entityManager.persist(courseTypeMap);
        entityManager.flush();
        entityManager.clear();

        List<NearbyCourseCandidateRow> results = courseRepository.findOfficialNearbyCourseCandidates();

        assertThat(results).hasSize(1);
        NearbyCourseCandidateRow row = results.getFirst();
        assertThat(row.courseId()).isEqualTo(course.getId());
        assertThat(row.coordinates()).isEqualTo(coordinates);
        assertThat(row.difficulty()).isEqualTo(Difficulty.MEDIUM);
        assertThat(row.courseType()).isEqualTo(CourseType.PARK);
    }

    @DisplayName("배열 형태 좌표 JSON도 Row 조회 시 Coordinates로 역직렬화한다")
    @Test
    void deserializesArrayShapedCoordinatesFromDatabase() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 3, 11, 2, 0);

        Course course = Course.builder()
                .title("Array Course")
                .status(CourseStatus.OFFICIAL)
                .distanceM(10000)
                .elevationGainM(40)
                .snapshotImageUrl("snapshot")
                .startLatitude(37.5665)
                .startLongitude(126.9780)
                .endLatitude(37.5700)
                .endLongitude(126.9820)
                .coordinates(new Coordinates(List.of(0.0), List.of(0.0), List.of(0.0)))
                .build();
        setCreatedAt(course, createdAt);
        course = courseRepository.save(course);

        CourseDifficulty courseDifficulty = CourseDifficulty.create(course, Difficulty.MEDIUM);
        CourseTypeMap courseTypeMap = CourseTypeMap.create(course, CourseType.PARK);
        setCreatedAt(courseDifficulty, createdAt);
        setCreatedAt(courseTypeMap, createdAt);

        entityManager.persist(courseDifficulty);
        entityManager.persist(courseTypeMap);
        entityManager.flush();

        updateCoordinatesJson(
                course.getId(),
                "[[37.5665,126.9780,12.0],[37.5668,126.9791,15.5]]"
        );
        entityManager.clear();

        List<NearbyCourseCandidateRow> results = courseRepository.findOfficialNearbyCourseCandidates();

        assertThat(results).hasSize(1);
        NearbyCourseCandidateRow row = results.getFirst();
        assertThat(row.coordinates()).isEqualTo(new Coordinates(
                List.of(37.5665, 37.5668),
                List.of(126.9780, 126.9791),
                List.of(12.0, 15.5)
        ));
    }

    private void setCreatedAt(Object entity, LocalDateTime createdAt) {
        try {
            Field field = entity.getClass().getSuperclass().getDeclaredField("createdAt");
            field.setAccessible(true);
            field.set(entity, createdAt);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to set createdAt", exception);
        }
    }

    private void updateCoordinatesJson(Long courseId, String coordinatesJson) {
        entityManager.createNativeQuery(
                        "update courses set coordinates = JSON '"
                                + coordinatesJson.replace("'", "''")
                                + "' where course_id = ?"
                )
                .setParameter(1, courseId)
                .executeUpdate();
        entityManager.flush();
    }
}
