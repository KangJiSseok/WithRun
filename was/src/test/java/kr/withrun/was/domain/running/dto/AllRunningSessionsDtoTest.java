package kr.withrun.was.domain.running.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import kr.withrun.was.domain.running.type.GhostResultStatus;
import kr.withrun.was.domain.running.type.RunningMode;
import kr.withrun.was.domain.user.type.TimeSlot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.RecordComponent;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("전체 러닝 세션 DTO")
class AllRunningSessionsDtoTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @DisplayName("요청 레코드의 구성 요소를 정확히 노출한다")
    @Test
    void exposesExactRequestComponents() {
        assertThat(componentNamesOf(AllRunningSessionsRequest.class))
                .containsExactly("userId", "pageSize", "cursor");
    }

    @DisplayName("사용자 ID는 양수여야 한다")
    @ParameterizedTest(name = "userId={0}")
    @ValueSource(longs = {0L, -1L})
    void rejectsNonPositiveUserId(long userId) {
        Set<ConstraintViolation<AllRunningSessionsRequest>> violations = validate(
                new AllRunningSessionsRequest(userId, 10, null)
        );

        assertOnlyViolationOn(violations, "userId");
    }

    @DisplayName("pageSize는 필수다")
    @Test
    void rejectsMissingPageSize() {
        Set<ConstraintViolation<AllRunningSessionsRequest>> violations = validate(
                new AllRunningSessionsRequest(1L, null, null)
        );

        assertOnlyViolationOn(violations, "pageSize");
    }

    @DisplayName("0 이하 pageSize는 거부한다")
    @ParameterizedTest(name = "pageSize={0}")
    @ValueSource(ints = {0, -1})
    void rejectsNonPositivePageSize(int pageSize) {
        Set<ConstraintViolation<AllRunningSessionsRequest>> violations = validate(
                new AllRunningSessionsRequest(1L, pageSize, null)
        );

        assertOnlyViolationOn(violations, "pageSize");
    }

    @DisplayName("pageSize 최대값 50은 허용한다")
    @Test
    void acceptsMaximumPageSize() {
        Set<ConstraintViolation<AllRunningSessionsRequest>> violations = validate(
                new AllRunningSessionsRequest(1L, 50, null)
        );

        assertThat(violations).isEmpty();
    }

    @DisplayName("허용 범위를 벗어난 pageSize는 거부한다")
    @Test
    void rejectsPageSizeAboveMaximum() {
        Set<ConstraintViolation<AllRunningSessionsRequest>> violations = validate(
                new AllRunningSessionsRequest(1L, 51, null)
        );

        assertOnlyViolationOn(violations, "pageSize");
    }

    @DisplayName("러닝 세션 아이템 응답의 구성 요소를 정확히 노출한다")
    @Test
    void exposesExactItemResponseComponents() {
        assertThat(componentNamesOf(AllRunningSessionItemResponse.class))
                .containsExactly(
                        "runningSessionId",
                        "distanceM",
                        "durationSec",
                        "caloriesKcal",
                        "snapshotImageUrl",
                        "elevationGainM",
                        "isCompleted",
                        "runningMode",
                        "ghostResultStatus",
                        "timeSlot"
                );
    }

    @DisplayName("러닝 세션 목록 응답의 구성 요소를 정확히 노출한다")
    @Test
    void exposesExactAllRunningSessionsResponseComponents() {
        assertThat(componentNamesOf(AllRunningSessionsResponse.class))
                .containsExactly("items", "hasMore", "nextCursor");
    }

    @DisplayName("러닝 세션 목록 응답은 카드 정보를 그대로 담는다")
    @Test
    void storesRunningSessionCardFields() {
        AllRunningSessionItemResponse item = new AllRunningSessionItemResponse(
                120L,
                7200,
                2100,
                430,
                "https://cdn.withrun.app/snapshots/120.png",
                42,
                true,
                RunningMode.GHOST,
                GhostResultStatus.WIN,
                TimeSlot.EVENING
        );
        AllRunningSessionsResponse response = new AllRunningSessionsResponse(List.of(item), true, "next-cursor");

        assertThat(response.items()).containsExactly(item);
        assertThat(response.hasMore()).isTrue();
        assertThat(response.nextCursor()).isEqualTo("next-cursor");
    }

    private Set<ConstraintViolation<AllRunningSessionsRequest>> validate(AllRunningSessionsRequest request) {
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
