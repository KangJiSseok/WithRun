package kr.withrun.was.domain.course.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.RecordComponent;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.type.CourseType;
import kr.withrun.was.global.common.type.Difficulty;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("북마크 코스 조회 DTO")
class BookmarkedCoursesDtoTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @DisplayName("요청 레코드의 구성 요소를 정확히 노출한다")
    @Test
    void exposesExactRequestComponents() {
        assertThat(componentNamesOf(BookmarkedCoursesRequest.class))
                .containsExactly("userId", "size", "cursor");
    }

    @DisplayName("사용자 ID는 양수여야 한다")
    @ParameterizedTest(name = "userId={0}")
    @ValueSource(longs = {0L, -1L})
    void rejectsNonPositiveUserId(long userId) {
        Set<ConstraintViolation<BookmarkedCoursesRequest>> violations = validate(
                new BookmarkedCoursesRequest(userId, 10, null)
        );

        assertOnlyViolationOn(violations, "userId");
    }

    @DisplayName("size는 필수다")
    @Test
    void rejectsMissingSize() {
        Set<ConstraintViolation<BookmarkedCoursesRequest>> violations = validate(
                new BookmarkedCoursesRequest(1L, null, null)
        );

        assertOnlyViolationOn(violations, "size");
    }

    @DisplayName("최대 size 50은 허용한다")
    @Test
    void acceptsMaximumSize() {
        Set<ConstraintViolation<BookmarkedCoursesRequest>> violations = validate(
                new BookmarkedCoursesRequest(1L, 50, null)
        );

        assertThat(violations).isEmpty();
    }

    @DisplayName("허용 범위를 벗어난 size 값은 거부한다")
    @ParameterizedTest(name = "size={0}")
    @ValueSource(ints = {0, -1, 51})
    void rejectsSizeOutsideSupportedRange(int size) {
        Set<ConstraintViolation<BookmarkedCoursesRequest>> violations = validate(
                new BookmarkedCoursesRequest(1L, size, null)
        );

        assertOnlyViolationOn(violations, "size");
    }

    @DisplayName("북마크 코스 아이템 응답의 구성 요소를 정확히 노출한다")
    @Test
    void exposesExactBookmarkedCourseItemResponseComponents() {
        assertThat(componentNamesOf(BookmarkedCourseItemResponse.class))
                .containsExactly(
                        "bookmarkId",
                        "bookmarkedAt",
                        "courseId",
                        "title",
                        "status",
                        "distanceM",
                        "elevationGainM",
                        "difficulty",
                        "courseTypes",
                        "snapshotImageUrl",
                        "isBookmarked",
                        "isRunnable"
                );
    }

    @DisplayName("북마크 코스 응답의 구성 요소를 정확히 노출한다")
    @Test
    void exposesExactBookmarkedCoursesResponseComponents() {
        assertThat(componentNamesOf(BookmarkedCoursesResponse.class))
                .containsExactly("items", "hasMore", "nextCursor");
    }

    @DisplayName("북마크 코스 응답은 카드 정보를 그대로 담는다")
    @Test
    void storesBookmarkCourseCardFields() {
        BookmarkedCourseItemResponse item = new BookmarkedCourseItemResponse(
                11L,
                LocalDateTime.of(2026, 3, 14, 10, 30),
                42L,
                "Han River",
                CourseStatus.OFFICIAL,
                10000,
                35,
                Difficulty.MEDIUM,
                List.of(CourseType.RIVERSIDE, CourseType.PARK),
                "https://example.com/course.jpg",
                true,
                true
        );
        BookmarkedCoursesResponse response = new BookmarkedCoursesResponse(List.of(item), true, "next-cursor");

        assertThat(response.items()).containsExactly(item);
        assertThat(response.hasMore()).isTrue();
        assertThat(response.nextCursor()).isEqualTo("next-cursor");
    }

    private Set<ConstraintViolation<BookmarkedCoursesRequest>> validate(BookmarkedCoursesRequest request) {
        return validator.validate(request);
    }

    private void assertOnlyViolationOn(Set<? extends ConstraintViolation<?>> violations, String propertyName) {
        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsExactly(propertyName);
    }

    private String[] componentNamesOf(Class<?> type) {
        RecordComponent[] components = type.getRecordComponents();
        assertThat(components).isNotNull();
        return java.util.Arrays.stream(components)
                .map(RecordComponent::getName)
                .toArray(String[]::new);
    }
}
