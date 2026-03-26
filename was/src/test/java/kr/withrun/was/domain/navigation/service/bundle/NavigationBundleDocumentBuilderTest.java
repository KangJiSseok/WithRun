package kr.withrun.was.domain.navigation.service.bundle;

import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleDocument;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleManeuver;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleVoiceInstruction;
import kr.withrun.was.domain.navigation.dto.internal.valhalla.NavigationManeuver;
import kr.withrun.was.domain.navigation.dto.internal.valhalla.RouteEdge;
import kr.withrun.was.domain.navigation.dto.internal.valhalla.SnappedRoutePoint;
import kr.withrun.was.domain.navigation.dto.internal.valhalla.ValhallaVoiceInstruction;
import kr.withrun.was.domain.navigation.dto.internal.valhalla.ValhallaRouteTraceResult;
import kr.withrun.was.domain.navigation.dto.internal.valhalla.ValhallaTraceAttributes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("네비게이션 번들 문서 빌더")
class NavigationBundleDocumentBuilderTest {

    @DisplayName("스냅된 shape 기준으로 maneuver 거리와 metadata 를 계산한다")
    @Test
    void buildsBundleDocumentWithComputedDistances() {
        NavigationBundleDocumentBuilder builder = new NavigationBundleDocumentBuilder(
                Clock.fixed(Instant.parse("2026-03-26T01:02:03Z"), ZoneOffset.UTC),
                "valhalla-test",
                "ko-KR"
        );

        NavigationBundleDocument document = builder.build(
                course(42L),
                new ValhallaRouteTraceResult(
                        List.of(
                                new SnappedRoutePoint(37.5665, 126.9780),
                                new SnappedRoutePoint(37.5670, 126.9790),
                                new SnappedRoutePoint(37.5675, 126.9800)
                        ),
                        List.of(
                                new NavigationManeuver(
                                        0,
                                        1,
                                        "직진하세요",
                                        "잠시 후 직진",
                                        "직진하세요",
                                        "계속 직진하세요",
                                        true,
                                        "직진",
                                        1,
                                        15,
                                        30,
                                        List.of(new ValhallaVoiceInstruction(80.0, "80m 앞 직진", "<speak>80m 앞 직진</speak>"))
                                ),
                                new NavigationManeuver(
                                        1,
                                        2,
                                        "목적지에 도착합니다",
                                        "잠시 후 도착",
                                        "도착합니다",
                                        "도착 후 종료하세요",
                                        false,
                                        "도착",
                                        15,
                                        30,
                                        45,
                                        List.of(new ValhallaVoiceInstruction(15.0, "잠시 후 도착", "<speak>잠시 후 도착</speak>"))
                                )
                        )
                ),
                new ValhallaTraceAttributes(
                        List.of(new RouteEdge(0, 2, 9.99, List.of("Wrong Length"))),
                        List.of(),
                        List.of()
                )
        );

        assertThat(document.metadata().generatedAt()).isEqualTo(Instant.parse("2026-03-26T01:02:03Z"));
        assertThat(document.metadata().valhallaVersion()).isEqualTo("valhalla-test");
        assertThat(document.metadata().language()).isEqualTo("ko-KR");
        assertThat(document.metadata().distanceMeters()).isGreaterThan(150d);

        assertThat(document.maneuvers()).extracting(
                NavigationBundleManeuver::instruction,
                NavigationBundleManeuver::verbalPreTransitionInstruction,
                NavigationBundleManeuver::type,
                NavigationBundleManeuver::bearingAfter,
                maneuver -> Math.round(maneuver.distanceMeters()),
                maneuver -> Math.round(maneuver.startDistanceMeters()),
                maneuver -> Math.round(maneuver.endDistanceMeters())
        ).containsExactly(
                org.assertj.core.groups.Tuple.tuple("직진하세요", "직진하세요", 1, 30, 104L, 0L, 104L),
                org.assertj.core.groups.Tuple.tuple("목적지에 도착합니다", "도착합니다", 15, 45, 104L, 104L, 208L)
        );
        assertThat(document.maneuvers().getFirst().voiceInstructions())
                .containsExactly(new NavigationBundleVoiceInstruction(80.0, "80m 앞 직진", "<speak>80m 앞 직진</speak>"));
        assertThat(document.maneuvers()).last().extracting(NavigationBundleManeuver::cumulativeDistanceMeters)
                .isEqualTo(document.metadata().distanceMeters());
    }

    private static Course course(Long id) {
        Course course = Course.builder()
                .title("Course " + id)
                .status(CourseStatus.OFFICIAL)
                .distanceM(5000)
                .elevationGainM(20)
                .startLatitude(37.5665)
                .startLongitude(126.9780)
                .endLatitude(37.5675)
                .endLongitude(126.9800)
                .coordinates(new Coordinates(List.of(37.5665, 37.5675), List.of(126.9780, 126.9800), List.of(10.0, 12.0)))
                .build();
        setId(course, id);
        return course;
    }

    private static void setId(Course course, Long id) {
        try {
            java.lang.reflect.Field field = Course.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(course, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Failed to set course id", exception);
        }
    }
}
