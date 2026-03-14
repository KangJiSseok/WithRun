package kr.withrun.was.domain.course.repository;

import jakarta.persistence.EntityManager;
import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.entity.CourseBookmark;
import kr.withrun.was.domain.course.repository.query.CourseBookmarkCustomRepository;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.domain.user.entity.User;
import kr.withrun.was.domain.user.type.Gender;
import kr.withrun.was.global.config.JpaAuditingConfig;
import kr.withrun.was.global.config.QuerydslConfig;
import kr.withrun.was.global.common.entity.BaseEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.config.import=",
        "spring.cloud.aws.parameterstore.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:course-bookmark;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;DATABASE_TO_LOWER=TRUE;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Import({JpaAuditingConfig.class, QuerydslConfig.class})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DisplayName("코스 북마크 리포지토리")
class CourseBookmarkRepositoryTest {

    @Autowired
    private CourseBookmarkRepository courseBookmarkRepository;

    @Autowired
    private EntityManager entityManager;

    @DisplayName("코스와 사용자 ID로 활성 북마크를 조회한다")
    @Test
    void findsActiveBookmarkByCourseIdAndUserId() {
        assertThat(courseBookmarkRepository).isInstanceOf(CourseBookmarkCustomRepository.class);
        assertThat(CourseBookmark.class.getSuperclass()).isNotEqualTo(BaseEntity.class);

        User user = persistUser("runner-a");
        Course course = persistCourse("bookmark-course-a");
        CourseBookmark bookmark = courseBookmarkRepository.save(CourseBookmark.create(course, user));

        entityManager.flush();
        entityManager.clear();

        CourseBookmark foundBookmark = courseBookmarkRepository
                .findBookmark(course.getId(), user.getId())
                .orElseThrow();

        assertThat(foundBookmark.getId()).isEqualTo(bookmark.getId());
        assertThat(foundBookmark.getCreatedAt()).isNotNull();
    }

    @DisplayName("코스와 사용자 ID로 북마크를 물리 삭제한다")
    @Test
    void deletesBookmarkPhysicallyByCourseIdAndUserId() {
        User user = persistUser("runner-b");
        Course course = persistCourse("bookmark-course-b");
        courseBookmarkRepository.save(CourseBookmark.create(course, user));
        entityManager.flush();

        courseBookmarkRepository.deleteBookmark(course.getId(), user.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(courseBookmarkRepository.findBookmark(course.getId(), user.getId())).isEmpty();
        assertThat(countBookmarkRows()).isZero();
    }

    @DisplayName("물리 삭제 후 같은 코스와 사용자로 다시 북마크할 수 있다")
    @Test
    void allowsAddRemoveAddWithoutUniqueConstraintProblems() {
        User user = persistUser("runner-c");
        Course course = persistCourse("bookmark-course-c");
        courseBookmarkRepository.save(CourseBookmark.create(course, user));
        entityManager.flush();

        courseBookmarkRepository.deleteBookmark(course.getId(), user.getId());
        entityManager.flush();

        CourseBookmark recreatedBookmark = courseBookmarkRepository.save(CourseBookmark.create(course, user));
        entityManager.flush();
        entityManager.clear();

        assertThat(recreatedBookmark.getId()).isNotNull();
        assertThat(courseBookmarkRepository.findBookmark(course.getId(), user.getId())).isPresent();
        assertThat(countBookmarkRows()).isEqualTo(1);
    }

    private long countBookmarkRows() {
        Number count = (Number) entityManager.createNativeQuery("select count(*) from course_bookmarks")
                .getSingleResult();
        return count.longValue();
    }

    private User persistUser(String nickname) {
        User user = instantiateUser();
        setField(user, "nickname", nickname);
        setField(user, "birthDate", LocalDate.of(1995, 3, 11));
        setField(user, "gender", Gender.MALE);
        setField(user, "height", 175.0);
        setField(user, "weight", 68.0);
        entityManager.persist(user);
        return user;
    }

    private User instantiateUser() {
        try {
            java.lang.reflect.Constructor<User> constructor = User.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to instantiate user", exception);
        }
    }

    private Course persistCourse(String title) {
        Course course = Course.builder()
                .title(title)
                .status(CourseStatus.OFFICIAL)
                .distanceM(5000)
                .elevationGainM(80)
                .snapshotImageUrl("snapshot")
                .startLatitude(37.5665)
                .startLongitude(126.9780)
                .endLatitude(37.5700)
                .endLongitude(126.9820)
                .coordinates(new Coordinates(List.of(37.5665), List.of(126.9780), List.of(10.0)))
                .build();
        entityManager.persist(course);
        return course;
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
