package kr.withrun.was.domain.running.dto;

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

@DisplayName("러닝 세션 종료 요청 DTO")
class UpdateRunningSessionRequestTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @DisplayName("요청 레코드의 구성 요소를 정확히 노출한다")
    @Test
    void exposesExactRequestComponents() {
        assertThat(componentNamesOf(UpdateRunningSessionRequest.class))
                .containsExactly(
                        "userId",
                        "distanceM",
                        "caloriesKcal",
                        "endLatitude",
                        "endLongitude",
                        "avgSpeedMps",
                        "durationSec",
                        "avgPaceSecPerKm",
                        "elevationGainM"
                );
    }

    @DisplayName("유효한 종료 요약값은 허용한다")
    @Test
    void acceptsValidCompletionPayload() {
        Set<ConstraintViolation<UpdateRunningSessionRequest>> violations = validate(validRequest());

        assertThat(violations).isEmpty();
    }

    @DisplayName("사용자 ID가 없으면 거부한다")
    @Test
    void rejectsMissingUserId() {
        Set<ConstraintViolation<UpdateRunningSessionRequest>> violations = validate(
                new UpdateRunningSessionRequest(
                        null,
                        10023,
                        642,
                        30.0,
                        150.0,
                        11.0,
                        2520,
                        2,
                        3
                )
        );

        assertOnlyViolationOn(violations, "userId");
    }


    @DisplayName("음수 누적값은 거부한다")
    @ParameterizedTest(name = "distanceM={0}")
    @ValueSource(ints = {-1, -10})
    void rejectsNegativeDistanceM(int distanceM) {
        Set<ConstraintViolation<UpdateRunningSessionRequest>> violations = validate(
                new UpdateRunningSessionRequest(
                        1L,
                        distanceM,
                        642,
                        30.0,
                        150.0,
                        11.0,
                        2520,
                        2,
                        3
                )
        );

        assertOnlyViolationOn(violations, "distanceM");
    }

    @DisplayName("0 이하 운동 시간은 거부한다")
    @ParameterizedTest(name = "durationSec={0}")
    @ValueSource(ints = {0, -1})
    void rejectsNonPositiveDurationSec(int durationSec) {
        Set<ConstraintViolation<UpdateRunningSessionRequest>> violations = validate(
                new UpdateRunningSessionRequest(
                        1L,
                        10023,
                        642,
                        30.0,
                        150.0,
                        11.0,
                        durationSec,
                        2,
                        3
                )
        );

        assertOnlyViolationOn(violations, "durationSec");
    }

    @DisplayName("허용 범위를 벗어난 종료 위도는 거부한다")
    @ParameterizedTest(name = "endLatitude={0}")
    @ValueSource(doubles = {-90.1, 90.1})
    void rejectsEndLatitudeOutsideSupportedRange(double endLatitude) {
        Set<ConstraintViolation<UpdateRunningSessionRequest>> violations = validate(
                new UpdateRunningSessionRequest(
                        1L,
                        10023,
                        642,
                        endLatitude,
                        150.0,
                        11.0,
                        2520,
                        2,
                        3
                )
        );

        assertOnlyViolationOn(violations, "endLatitude");
    }

    @DisplayName("허용 범위를 벗어난 종료 경도는 거부한다")
    @ParameterizedTest(name = "endLongitude={0}")
    @ValueSource(doubles = {-180.1, 180.1})
    void rejectsEndLongitudeOutsideSupportedRange(double endLongitude) {
        Set<ConstraintViolation<UpdateRunningSessionRequest>> violations = validate(
                new UpdateRunningSessionRequest(
                        1L,
                        10023,
                        642,
                        30.0,
                        endLongitude,
                        11.0,
                        2520,
                        2,
                        3
                )
        );

        assertOnlyViolationOn(violations, "endLongitude");
    }

    @DisplayName("응답 레코드의 구성 요소를 정확히 노출한다")
    @Test
    void exposesExactResponseComponents() {
        assertThat(componentNamesOf(UpdateRunningSessionResponse.class))
                .containsExactly(
                        "runningSessionId",
                        "isCompleted",
                        "isPublic",
                        "startedAt",
                        "endedAt",
                        "distanceM",
                        "caloriesKcal",
                        "endLatitude",
                        "endLongitude",
                        "avgSpeedMps",
                        "durationSec",
                        "avgPaceSecPerKm",
                        "startLatitude",
                        "startLongitude",
                        "elevationGainM"
                );
    }

    private UpdateRunningSessionRequest validRequest() {
        return new UpdateRunningSessionRequest(
                1L,
                10023,
                642,
                30.0,
                150.0,
                11.0,
                2520,
                2,
                3
        );
    }

    private Set<ConstraintViolation<UpdateRunningSessionRequest>> validate(UpdateRunningSessionRequest request) {
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
