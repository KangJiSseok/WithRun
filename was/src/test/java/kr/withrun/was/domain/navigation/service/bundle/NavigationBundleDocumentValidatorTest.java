package kr.withrun.was.domain.navigation.service.bundle;

import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleDocument;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleManeuver;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleMetadata;
import kr.withrun.was.domain.navigation.dto.internal.valhalla.SnappedRoutePoint;
import kr.withrun.was.domain.navigation.exception.NavigationBundleGenerationException;
import kr.withrun.was.domain.navigation.type.NavigationBundleFailureCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("네비게이션 번들 문서 검증기")
class NavigationBundleDocumentValidatorTest {

    private final NavigationBundleDocumentValidator validator = new NavigationBundleDocumentValidator();

    @DisplayName("trace_attributes 가 비어 있어도 핵심 번들 구조가 정상이면 통과한다")
    @Test
    void allowsBundleWithoutOptionalTraceAttributes() {
        assertThatCode(() -> validator.validate(validDocument())).doesNotThrowAnyException();
    }

    @DisplayName("maneuver 범위가 역전되면 실패한다")
    @Test
    void rejectsInvalidManeuverRange() {
        NavigationBundleDocument document = new NavigationBundleDocument(
                List.of(
                        new SnappedRoutePoint(37.5665, 126.9780),
                        new SnappedRoutePoint(37.5670, 126.9790),
                        new SnappedRoutePoint(37.5675, 126.9800)
                ),
                List.of(new NavigationBundleManeuver(
                        "잘못된 maneuver",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        List.of(),
                        2,
                        1,
                        50d,
                        50d,
                        0d,
                        50d
                )),
                new NavigationBundleMetadata(50d, Instant.parse("2026-03-26T01:02:03Z"), "valhalla-test", "ko-KR"),
                List.of(),
                List.of(),
                List.of()
        );

        assertThatThrownBy(() -> validator.validate(document))
                .isInstanceOf(NavigationBundleGenerationException.class)
                .extracting("failureCode")
                .isEqualTo(NavigationBundleFailureCode.BUNDLE_VALIDATION_FAILED);
    }

    private static NavigationBundleDocument validDocument() {
        return new NavigationBundleDocument(
                List.of(
                        new SnappedRoutePoint(37.5665, 126.9780),
                        new SnappedRoutePoint(37.5670, 126.9790),
                        new SnappedRoutePoint(37.5675, 126.9800)
                ),
                List.of(
                        new NavigationBundleManeuver(
                                "직진하세요",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                List.of(),
                                0,
                                1,
                                104d,
                                104d,
                                0d,
                                104d
                        ),
                        new NavigationBundleManeuver(
                                "도착합니다",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                List.of(),
                                1,
                                2,
                                105d,
                                209d,
                                104d,
                                209d
                        )
                ),
                new NavigationBundleMetadata(209d, Instant.parse("2026-03-26T01:02:03Z"), "valhalla-test", "ko-KR"),
                List.of(),
                List.of(),
                List.of()
        );
    }
}
