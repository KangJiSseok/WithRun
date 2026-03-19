package kr.withrun.was.domain.course.repository;

import jakarta.persistence.EntityManager;
import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.entity.CourseGhostLeaderboard;
import kr.withrun.was.domain.course.repository.query.CourseGhostLeaderboardCustomRepository;
import kr.withrun.was.domain.course.repository.query.dto.CourseGhostLeaderboardRankRow;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.domain.course.vo.GeoPoint;
import kr.withrun.was.domain.running.entity.RunningSession;
import kr.withrun.was.domain.running.type.RunningMode;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.type.Gender;
import kr.withrun.was.global.config.JpaAuditingConfig;
import kr.withrun.was.global.config.QuerydslConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.config.import=",
        "spring.cloud.aws.parameterstore.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:course-ghost-leaderboard;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("코스 고스트 리더보드 리포지토리")
class CourseGhostLeaderboardRepositoryTest {

    @Autowired
    private CourseGhostLeaderboardRepository courseGhostLeaderboardRepository;

    @Autowired
    private EntityManager entityManager;

    @DisplayName("point 기준 rank와 페이지 오프셋을 적용해 리더보드를 조회한다")
    @Test
    void findsRankedRowsByCourseIdWithPagination() {
        assertThat(courseGhostLeaderboardRepository).isInstanceOf(CourseGhostLeaderboardCustomRepository.class);

        Course course = persistCourse("Leader Course");
        Course otherCourse = persistCourse("Other Course");
        User userA = persistUser("runner-a");
        User userB = persistUser("runner-b");
        User userC = persistUser("runner-c");
        User userD = persistUser("runner-d");
        User userE = persistUser("runner-e");

        persistLeaderboard(course, userA, 500, LocalDateTime.of(2026, 3, 18, 10, 0));
        persistLeaderboard(course, userB, 300, LocalDateTime.of(2026, 3, 18, 10, 1));
        persistLeaderboard(course, userC, 300, LocalDateTime.of(2026, 3, 18, 10, 2));
        persistLeaderboard(course, userD, 100, LocalDateTime.of(2026, 3, 18, 10, 3));
        persistLeaderboard(otherCourse, userE, 999, LocalDateTime.of(2026, 3, 18, 10, 4));

        entityManager.flush();
        entityManager.clear();

        List<CourseGhostLeaderboardRankRow> firstPageRows = courseGhostLeaderboardRepository.findRankedRowsByCourseId(
                course.getId(),
                0,
                3
        );

        assertThat(firstPageRows).hasSize(3);
        assertThat(firstPageRows).extracting(CourseGhostLeaderboardRankRow::point).containsExactly(500, 300, 300);
        assertThat(firstPageRows).extracting(CourseGhostLeaderboardRankRow::rank).containsExactly(1L, 2L, 2L);
        assertThat(firstPageRows).extracting(CourseGhostLeaderboardRankRow::nickname).containsExactly("runner-a", "runner-b", "runner-c");

        List<CourseGhostLeaderboardRankRow> secondPageRows = courseGhostLeaderboardRepository.findRankedRowsByCourseId(
                course.getId(),
                1,
                2
        );

        assertThat(secondPageRows).hasSize(2);
        assertThat(secondPageRows).extracting(CourseGhostLeaderboardRankRow::point).containsExactly(300, 100);
        assertThat(secondPageRows).extracting(CourseGhostLeaderboardRankRow::rank).containsExactly(2L, 4L);
        assertThat(courseGhostLeaderboardRepository.countByCourseId(course.getId())).isEqualTo(4L);
    }

    @DisplayName("삭제된 리더보드 항목은 count에서 제외한다")
    @Test
    void excludesDeletedLeaderboardRowsFromCount() {
        Course course = persistCourse("Deleted Row Course");
        User user = persistUser("runner-delete");
        CourseGhostLeaderboard leaderboard = persistLeaderboard(
                course,
                user,
                120,
                LocalDateTime.of(2026, 3, 18, 11, 0)
        );
        leaderboard.delete();

        entityManager.flush();
        entityManager.clear();

        assertThat(courseGhostLeaderboardRepository.countByCourseId(course.getId())).isZero();
    }

    private Course persistCourse(String title) {
        Course course = Course.builder()
                .title(title)
                .status(CourseStatus.OFFICIAL)
                .distanceM(5000)
                .elevationGainM(100)
                .snapshotImageUrl("https://example.com/course.jpg")
                .startLatitude(37.5665)
                .startLongitude(126.9780)
                .endLatitude(37.5700)
                .endLongitude(126.9820)
                .coordinates(new Coordinates(List.of(
                        new GeoPoint(37.5665, 126.9780, 10.0),
                        new GeoPoint(37.5700, 126.9820, 15.0)
                )))
                .build();
        entityManager.persist(course);
        return course;
    }

    private User persistUser(String nickname) {
        User user = instantiate(User.class);
        setField(user, "nickname", nickname);
        setField(user, "birthDate", LocalDate.of(1995, 3, 11));
        setField(user, "gender", Gender.MALE);
        setField(user, "height", 175.0);
        setField(user, "weight", 68.0);
        entityManager.persist(user);
        return user;
    }

    private CourseGhostLeaderboard persistLeaderboard(
            Course course,
            User user,
            int point,
            LocalDateTime startedAt
    ) {
        RunningSession runningSession = RunningSession.start(
                user,
                RunningMode.COURSE,
                course,
                null,
                startedAt,
                37.5,
                127.0
        );
        entityManager.persist(runningSession);

        CourseGhostLeaderboard leaderboard = CourseGhostLeaderboard.create(user, course, runningSession, point);
        entityManager.persist(leaderboard);
        return leaderboard;
    }

    private <T> T instantiate(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to instantiate " + type.getSimpleName(), exception);
        }
    }

    private void setField(Object target, String fieldName, Object value) {
        Class<?> currentClass = target.getClass();
        while (currentClass != null) {
            try {
                Field field = currentClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException exception) {
                currentClass = currentClass.getSuperclass();
            } catch (IllegalAccessException exception) {
                throw new IllegalStateException("Failed to set field " + fieldName, exception);
            }
        }

        throw new IllegalArgumentException("Field not found: " + fieldName);
    }
}
