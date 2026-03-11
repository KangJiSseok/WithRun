package kr.withrun.was.domain.course.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.RecordComponent;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("주변 코스 요청 DTO")
class NearbyCoursesRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @DisplayName("요청 레코드의 구성 요소를 정확히 노출한다")
    @Test
    void exposesExactRequestComponents() {
        assertThat(componentNamesOf(NearbyCoursesRequest.class))
                .containsExactly("latitude", "longitude", "radiusM", "targetDistanceM", "cursor");
    }

    @DisplayName("선택 커서 없이도 요청을 허용한다")
    @Test
    void acceptsRequestWhenOptionalCursorIsOmitted() {
        Set<ConstraintViolation<NearbyCoursesRequest>> violations = validate(
                new NearbyCoursesRequest(37.5665, 126.9780, 3000, 10000, null)
        );

        assertThat(violations).isEmpty();
    }

    @DisplayName("DTO 계층에서는 커서를 해석하거나 검증하지 않는다")
    @Test
    void doesNotDecodeOrValidateCursorAtDtoLayer() {
        Set<ConstraintViolation<NearbyCoursesRequest>> violations = validate(
                new NearbyCoursesRequest(37.5665, 126.9780, 3000, 10000, "%%%")
        );

        assertThat(violations).isEmpty();
    }

    @DisplayName("반경이 없으면 거부한다")
    @Test
    void rejectsMissingRadiusM() {
        Set<ConstraintViolation<NearbyCoursesRequest>> violations = validate(
                new NearbyCoursesRequest(37.5665, 126.9780, null, 10000, null)
        );

        assertOnlyViolationOn(violations, "radiusM");
    }

    @DisplayName("허용 범위를 벗어난 반경 값은 거부한다")
    @ParameterizedTest(name = "radiusM={0}")
    @ValueSource(ints = {0, 6000})
    void rejectsRadiusMOutsideSupportedRange(int radiusM) {
        Set<ConstraintViolation<NearbyCoursesRequest>> violations = validate(
                new NearbyCoursesRequest(37.5665, 126.9780, radiusM, 10000, null)
        );

        assertOnlyViolationOn(violations, "radiusM");
    }

    @DisplayName("반경이 범위 안에 있으면 1000 단위가 아니어도 허용한다")
    @Test
    void acceptsRadiusMThatIsNotMultipleOf1000WhenItIsWithinRange() {
        Set<ConstraintViolation<NearbyCoursesRequest>> violations = validate(
                new NearbyCoursesRequest(37.5665, 126.9780, 1500, 10000, null)
        );

        assertThat(violations).isEmpty();
    }

    @DisplayName("목표 거리가 없으면 거부한다")
    @Test
    void rejectsMissingTargetDistanceM() {
        Set<ConstraintViolation<NearbyCoursesRequest>> violations = validate(
                new NearbyCoursesRequest(37.5665, 126.9780, 3000, null, null)
        );

        assertOnlyViolationOn(violations, "targetDistanceM");
    }

    @DisplayName("0 이하의 목표 거리 값은 거부한다")
    @ParameterizedTest(name = "targetDistanceM={0}")
    @ValueSource(ints = {0, -1})
    void rejectsNonPositiveTargetDistanceM(int targetDistanceM) {
        Set<ConstraintViolation<NearbyCoursesRequest>> violations = validate(
                new NearbyCoursesRequest(37.5665, 126.9780, 3000, targetDistanceM, null)
        );

        assertOnlyViolationOn(violations, "targetDistanceM");
    }

    @DisplayName("허용 범위를 벗어난 위도 값은 거부한다")
    @ParameterizedTest(name = "latitude={0}")
    @ValueSource(doubles = {-90.1, 90.1})
    void rejectsLatitudeOutsideSupportedRange(double latitude) {
        Set<ConstraintViolation<NearbyCoursesRequest>> violations = validate(
                new NearbyCoursesRequest(latitude, 126.9780, 3000, 10000, null)
        );

        assertOnlyViolationOn(violations, "latitude");
    }

    @DisplayName("허용 범위를 벗어난 경도 값은 거부한다")
    @ParameterizedTest(name = "longitude={0}")
    @ValueSource(doubles = {-180.1, 180.1})
    void rejectsLongitudeOutsideSupportedRange(double longitude) {
        Set<ConstraintViolation<NearbyCoursesRequest>> violations = validate(
                new NearbyCoursesRequest(37.5665, longitude, 3000, 10000, null)
        );

        assertOnlyViolationOn(violations, "longitude");
    }

    @DisplayName("주변 코스 아이템 응답의 구성 요소를 정확히 노출한다")
    @Test
    void exposesExactNearbyCourseItemResponseComponents() {
        assertThat(componentNamesOf(NearbyCourseItemResponse.class))
                .containsExactly(
                        "courseId",
                        "title",
                        "status",
                        "distanceM",
                        "elevationGainM",
                        "startLatitude",
                        "startLongitude",
                        "endLatitude",
                        "endLongitude",
                        "distanceFromUserM",
                        "difficulty",
                        "courseTypes",
                        "snapshotImageUrl"
                );
    }

    @DisplayName("주변 코스 응답의 구성 요소를 정확히 노출한다")
    @Test
    void exposesExactNearbyCoursesResponseComponents() {
        assertThat(componentNamesOf(NearbyCoursesResponse.class))
                .containsExactly("items", "hasMore", "nextCursor", "suggestedNextRadiusM");
    }

    private Set<ConstraintViolation<NearbyCoursesRequest>> validate(NearbyCoursesRequest request) {
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
