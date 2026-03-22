package kr.withrun.was.domain.course.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.RecordComponent;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("주변 코스 요청 DTO")
class NearbyCoursesRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @DisplayName("요청 레코드의 구성 요소를 정확히 노출한다")
    @Test
    void exposesExactRequestComponents() {
        assertThat(componentNamesOf(NearbyCoursesRequest.class))
                .containsExactly(
                        "latitude",
                        "longitude",
                        "targetLatitude",
                        "targetLongitude",
                        "radiusM",
                        "preferredDistanceMs",
                        "pageSize",
                        "cursor"
                );
    }

    @DisplayName("선택 커서 없이도 요청을 허용한다")
    @Test
    void acceptsRequestWhenOptionalCursorIsOmitted() {
        Set<ConstraintViolation<NearbyCoursesRequest>> violations = validate(
                request(3000, 37.5700, 126.9820, defaultRanges(), null, null)
        );

        assertThat(violations).isEmpty();
    }

    @DisplayName("DTO 계층에서는 커서를 해석하거나 검증하지 않는다")
    @Test
    void doesNotDecodeOrValidateCursorAtDtoLayer() {
        Set<ConstraintViolation<NearbyCoursesRequest>> violations = validate(
                request(3000, 37.5700, 126.9820, defaultRanges(), null, "%%%")
        );

        assertThat(violations).isEmpty();
    }

    @DisplayName("반경이 없으면 거부한다")
    @Test
    void rejectsMissingRadiusM() {
        Set<ConstraintViolation<NearbyCoursesRequest>> violations = validate(
                request(null, 37.5700, 126.9820, defaultRanges(), null, null)
        );

        assertOnlyViolationOn(violations, "radiusM");
    }

    @DisplayName("허용 범위를 벗어난 반경 값을 거부한다")
    @ParameterizedTest(name = "radiusM={0}")
    @ValueSource(ints = {0, 6000})
    void rejectsRadiusMOutsideSupportedRange(int radiusM) {
        Set<ConstraintViolation<NearbyCoursesRequest>> violations = validate(
                request(radiusM, 37.5700, 126.9820, defaultRanges(), null, null)
        );

        assertOnlyViolationOn(violations, "radiusM");
    }

    @DisplayName("반경 범위 안에 있으면 1000 단위가 아니어도 허용한다")
    @Test
    void acceptsRadiusMThatIsNotMultipleOf1000WhenItIsWithinRange() {
        Set<ConstraintViolation<NearbyCoursesRequest>> violations = validate(
                request(1500, 37.5700, 126.9820, defaultRanges(), null, null)
        );

        assertThat(violations).isEmpty();
    }

    @DisplayName("타겟 위도가 없으면 거부한다")
    @Test
    void rejectsMissingTargetLatitude() {
        Set<ConstraintViolation<NearbyCoursesRequest>> violations = validate(
                request(3000, null, 126.9820, defaultRanges(), null, null)
        );

        assertOnlyViolationOn(violations, "targetLatitude");
    }

    @DisplayName("타겟 경도가 없으면 거부한다")
    @Test
    void rejectsMissingTargetLongitude() {
        Set<ConstraintViolation<NearbyCoursesRequest>> violations = validate(
                request(3000, 37.5700, null, defaultRanges(), null, null)
        );

        assertOnlyViolationOn(violations, "targetLongitude");
    }

    @DisplayName("선호 거리 목록이 없으면 거부한다")
    @Test
    void rejectsMissingPreferredDistanceMs() {
        Set<ConstraintViolation<NearbyCoursesRequest>> violations = validate(
                request(3000, 37.5700, 126.9820, null, null, null)
        );

        assertOnlyViolationOn(violations, "preferredDistanceMs");
    }

    @DisplayName("허용 범위를 벗어난 위도 값을 거부한다")
    @ParameterizedTest(name = "latitude={0}")
    @ValueSource(doubles = {-90.1, 90.1})
    void rejectsLatitudeOutsideSupportedRange(double latitude) {
        Set<ConstraintViolation<NearbyCoursesRequest>> violations = validate(
                request(latitude, 126.9780, 37.5700, 126.9820, 3000, defaultRanges(), null, null)
        );

        assertOnlyViolationOn(violations, "latitude");
    }

    @DisplayName("허용 범위를 벗어난 경도 값을 거부한다")
    @ParameterizedTest(name = "longitude={0}")
    @ValueSource(doubles = {-180.1, 180.1})
    void rejectsLongitudeOutsideSupportedRange(double longitude) {
        Set<ConstraintViolation<NearbyCoursesRequest>> violations = validate(
                request(37.5665, longitude, 37.5700, 126.9820, 3000, defaultRanges(), null, null)
        );

        assertOnlyViolationOn(violations, "longitude");
    }

    @DisplayName("허용 범위를 벗어난 타겟 위도 값을 거부한다")
    @ParameterizedTest(name = "targetLatitude={0}")
    @ValueSource(doubles = {-90.1, 90.1})
    void rejectsTargetLatitudeOutsideSupportedRange(double targetLatitude) {
        Set<ConstraintViolation<NearbyCoursesRequest>> violations = validate(
                request(3000, targetLatitude, 126.9820, defaultRanges(), null, null)
        );

        assertOnlyViolationOn(violations, "targetLatitude");
    }

    @DisplayName("허용 범위를 벗어난 타겟 경도 값을 거부한다")
    @ParameterizedTest(name = "targetLongitude={0}")
    @ValueSource(doubles = {-180.1, 180.1})
    void rejectsTargetLongitudeOutsideSupportedRange(double targetLongitude) {
        Set<ConstraintViolation<NearbyCoursesRequest>> violations = validate(
                request(3000, 37.5700, targetLongitude, defaultRanges(), null, null)
        );

        assertOnlyViolationOn(violations, "targetLongitude");
    }

    @DisplayName("페이지 크기가 없으면 기본값 위임을 위해 허용한다")
    @Test
    void acceptsMissingPageSize() {
        Set<ConstraintViolation<NearbyCoursesRequest>> violations = validate(
                request(3000, 37.5700, 126.9820, defaultRanges(), null, null)
        );

        assertThat(violations).isEmpty();
    }

    @DisplayName("최대 페이지 크기 10을 허용한다")
    @Test
    void acceptsMaximumPageSize() {
        Set<ConstraintViolation<NearbyCoursesRequest>> violations = validate(
                request(3000, 37.5700, 126.9820, defaultRanges(), 10, null)
        );

        assertThat(violations).isEmpty();
    }

    @DisplayName("허용 범위를 벗어난 페이지 크기 값을 거부한다")
    @ParameterizedTest(name = "pageSize={0}")
    @ValueSource(ints = {0, -1, 11})
    void rejectsPageSizeOutsideSupportedRange(int pageSize) {
        Set<ConstraintViolation<NearbyCoursesRequest>> violations = validate(
                request(3000, 37.5700, 126.9820, defaultRanges(), pageSize, null)
        );

        assertOnlyViolationOn(violations, "pageSize");
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
                        "distanceFromTargetM",
                        "distanceFromUserM",
                        "difficulty",
                        "courseTypes",
                        "likeCount",
                        "bookmarkCount",
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

    private NearbyCoursesRequest request(
            Integer radiusM,
            Double targetLatitude,
            Double targetLongitude,
            List<PreferredDistanceRange> preferredDistanceMs,
            Integer pageSize,
            String cursor
    ) {
        return request(37.5665, 126.9780, targetLatitude, targetLongitude, radiusM, preferredDistanceMs, pageSize, cursor);
    }

    private NearbyCoursesRequest request(
            Double latitude,
            Double longitude,
            Double targetLatitude,
            Double targetLongitude,
            Integer radiusM,
            List<PreferredDistanceRange> preferredDistanceMs,
            Integer pageSize,
            String cursor
    ) {
        return new NearbyCoursesRequest(
                latitude,
                longitude,
                targetLatitude,
                targetLongitude,
                radiusM,
                preferredDistanceMs,
                pageSize,
                cursor
        );
    }

    private List<PreferredDistanceRange> defaultRanges() {
        return List.of(
                new PreferredDistanceRange(1, 3000),
                new PreferredDistanceRange(3001, 5000)
        );
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
