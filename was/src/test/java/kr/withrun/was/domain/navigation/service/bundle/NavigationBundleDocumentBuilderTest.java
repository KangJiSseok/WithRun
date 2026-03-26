package kr.withrun.was.domain.navigation.service.bundle;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleDocument;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleManeuver;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleManeuverSampleAction;
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
import java.util.ArrayList;
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
                NavigationBundleManeuver::sampleAction,
                maneuver -> Math.round(maneuver.distanceMeters()),
                maneuver -> Math.round(maneuver.startDistanceMeters()),
                maneuver -> Math.round(maneuver.endDistanceMeters())
        ).containsExactly(
                org.assertj.core.groups.Tuple.tuple("직진하세요", "직진하세요", 1, 30, NavigationBundleManeuverSampleAction.STRAIGHT, 104L, 0L, 104L),
                org.assertj.core.groups.Tuple.tuple("목적지에 도착합니다", "도착합니다", 15, 45, NavigationBundleManeuverSampleAction.LEFT, 104L, 104L, 208L)
        );
        assertThat(document.maneuvers().getFirst().voiceInstructions())
                .containsExactly(new NavigationBundleVoiceInstruction(80.0, "80m 앞 직진", "<speak>80m 앞 직진</speak>"));
        assertThat(document.maneuvers()).last().extracting(NavigationBundleManeuver::cumulativeDistanceMeters)
                .isEqualTo(document.metadata().distanceMeters());
    }

    @DisplayName("Valhalla maneuver type 을 압축 계열과 sampleAction 으로 정규화한다")
    @Test
    void compressesManeuverTypesIntoFamilies() {
        NavigationBundleDocumentBuilder builder = new NavigationBundleDocumentBuilder(
                Clock.fixed(Instant.parse("2026-03-26T01:02:03Z"), ZoneOffset.UTC),
                "valhalla-test",
                "ko-KR"
        );

        NavigationBundleDocument document = builder.build(
                course(7L),
                new ValhallaRouteTraceResult(
                        shapePoints(47),
                        maneuversForTypes(
                                0, 1, 2, 3, 4, 5, 6, 7, 8,
                                9, 10, 11, 12, 13, 14, 15, 16,
                                17, 18, 19, 20, 21, 22, 23, 24,
                                25, 26, 27, 28, 29, 30, 31, 32,
                                33, 34, 35, 36, 37, 38, 39, 40,
                                41, 42, 43, 44, 45
                        )
                ),
                new ValhallaTraceAttributes(List.of(), List.of(), List.of())
        );

        assertThat(document.maneuvers()).extracting(
                NavigationBundleManeuver::type,
                NavigationBundleManeuver::sampleAction
        ).containsExactly(
                org.assertj.core.groups.Tuple.tuple(0, NavigationBundleManeuverSampleAction.STRAIGHT),
                org.assertj.core.groups.Tuple.tuple(1, NavigationBundleManeuverSampleAction.STRAIGHT),
                org.assertj.core.groups.Tuple.tuple(2, NavigationBundleManeuverSampleAction.STRAIGHT),
                org.assertj.core.groups.Tuple.tuple(3, NavigationBundleManeuverSampleAction.STRAIGHT),
                org.assertj.core.groups.Tuple.tuple(4, NavigationBundleManeuverSampleAction.ARRIVAL),
                org.assertj.core.groups.Tuple.tuple(5, NavigationBundleManeuverSampleAction.ARRIVAL),
                org.assertj.core.groups.Tuple.tuple(6, NavigationBundleManeuverSampleAction.ARRIVAL),
                org.assertj.core.groups.Tuple.tuple(7, NavigationBundleManeuverSampleAction.STRAIGHT),
                org.assertj.core.groups.Tuple.tuple(8, NavigationBundleManeuverSampleAction.STRAIGHT),
                org.assertj.core.groups.Tuple.tuple(9, NavigationBundleManeuverSampleAction.RIGHT),
                org.assertj.core.groups.Tuple.tuple(10, NavigationBundleManeuverSampleAction.RIGHT),
                org.assertj.core.groups.Tuple.tuple(11, NavigationBundleManeuverSampleAction.RIGHT),
                org.assertj.core.groups.Tuple.tuple(12, NavigationBundleManeuverSampleAction.UTURN),
                org.assertj.core.groups.Tuple.tuple(13, NavigationBundleManeuverSampleAction.UTURN),
                org.assertj.core.groups.Tuple.tuple(14, NavigationBundleManeuverSampleAction.LEFT),
                org.assertj.core.groups.Tuple.tuple(15, NavigationBundleManeuverSampleAction.LEFT),
                org.assertj.core.groups.Tuple.tuple(16, NavigationBundleManeuverSampleAction.LEFT),
                org.assertj.core.groups.Tuple.tuple(17, NavigationBundleManeuverSampleAction.STRAIGHT),
                org.assertj.core.groups.Tuple.tuple(18, NavigationBundleManeuverSampleAction.RIGHT),
                org.assertj.core.groups.Tuple.tuple(19, NavigationBundleManeuverSampleAction.LEFT),
                org.assertj.core.groups.Tuple.tuple(20, NavigationBundleManeuverSampleAction.RIGHT),
                org.assertj.core.groups.Tuple.tuple(21, NavigationBundleManeuverSampleAction.LEFT),
                org.assertj.core.groups.Tuple.tuple(22, NavigationBundleManeuverSampleAction.STRAIGHT),
                org.assertj.core.groups.Tuple.tuple(23, NavigationBundleManeuverSampleAction.RIGHT),
                org.assertj.core.groups.Tuple.tuple(24, NavigationBundleManeuverSampleAction.LEFT),
                org.assertj.core.groups.Tuple.tuple(25, NavigationBundleManeuverSampleAction.STRAIGHT),
                org.assertj.core.groups.Tuple.tuple(26, NavigationBundleManeuverSampleAction.STRAIGHT),
                org.assertj.core.groups.Tuple.tuple(27, NavigationBundleManeuverSampleAction.STRAIGHT),
                org.assertj.core.groups.Tuple.tuple(28, NavigationBundleManeuverSampleAction.STRAIGHT),
                org.assertj.core.groups.Tuple.tuple(29, NavigationBundleManeuverSampleAction.STRAIGHT),
                org.assertj.core.groups.Tuple.tuple(30, NavigationBundleManeuverSampleAction.STRAIGHT),
                org.assertj.core.groups.Tuple.tuple(31, NavigationBundleManeuverSampleAction.STRAIGHT),
                org.assertj.core.groups.Tuple.tuple(32, NavigationBundleManeuverSampleAction.STRAIGHT),
                org.assertj.core.groups.Tuple.tuple(33, NavigationBundleManeuverSampleAction.STRAIGHT),
                org.assertj.core.groups.Tuple.tuple(34, NavigationBundleManeuverSampleAction.STRAIGHT),
                org.assertj.core.groups.Tuple.tuple(35, NavigationBundleManeuverSampleAction.STRAIGHT),
                org.assertj.core.groups.Tuple.tuple(36, NavigationBundleManeuverSampleAction.STRAIGHT),
                org.assertj.core.groups.Tuple.tuple(37, NavigationBundleManeuverSampleAction.RIGHT),
                org.assertj.core.groups.Tuple.tuple(38, NavigationBundleManeuverSampleAction.LEFT),
                org.assertj.core.groups.Tuple.tuple(39, NavigationBundleManeuverSampleAction.STRAIGHT),
                org.assertj.core.groups.Tuple.tuple(40, NavigationBundleManeuverSampleAction.STRAIGHT),
                org.assertj.core.groups.Tuple.tuple(41, NavigationBundleManeuverSampleAction.STRAIGHT),
                org.assertj.core.groups.Tuple.tuple(42, NavigationBundleManeuverSampleAction.STRAIGHT),
                org.assertj.core.groups.Tuple.tuple(43, NavigationBundleManeuverSampleAction.STRAIGHT),
                org.assertj.core.groups.Tuple.tuple(44, NavigationBundleManeuverSampleAction.STRAIGHT),
                org.assertj.core.groups.Tuple.tuple(45, NavigationBundleManeuverSampleAction.STRAIGHT)
        );
    }

    @DisplayName("enum 기반 maneuver 메타데이터도 기존 문자열 wire format 으로 직렬화된다")
    @Test
    void serializesEnumBackedManeuverMetadataAsExistingWireValues() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        List<NavigationBundleManeuver> maneuvers = List.of(
                bundleManeuver(18, NavigationBundleManeuverSampleAction.RIGHT),
                bundleManeuver(15, NavigationBundleManeuverSampleAction.LEFT),
                bundleManeuver(12, NavigationBundleManeuverSampleAction.UTURN),
                bundleManeuver(4, NavigationBundleManeuverSampleAction.ARRIVAL),
                bundleManeuver(1, NavigationBundleManeuverSampleAction.STRAIGHT)
        );

        String json = objectMapper.writeValueAsString(maneuvers);
        NavigationBundleManeuver[] restored = objectMapper.readValue(json, NavigationBundleManeuver[].class);

        assertThat(json).doesNotContain("\"actionFamily\"");
        assertThat(json).contains("\"sampleAction\":\"우회전\"");
        assertThat(json).contains("\"sampleAction\":\"좌회전\"");
        assertThat(json).contains("\"sampleAction\":\"유턴\"");
        assertThat(json).contains("\"sampleAction\":\"도착\"");
        assertThat(json).contains("\"sampleAction\":\"직진\"");
        assertThat(restored).extracting(NavigationBundleManeuver::sampleAction).containsExactly(
                NavigationBundleManeuverSampleAction.RIGHT,
                NavigationBundleManeuverSampleAction.LEFT,
                NavigationBundleManeuverSampleAction.UTURN,
                NavigationBundleManeuverSampleAction.ARRIVAL,
                NavigationBundleManeuverSampleAction.STRAIGHT
        );
    }

    private static NavigationBundleManeuver bundleManeuver(
            int type,
            NavigationBundleManeuverSampleAction sampleAction
    ) {
        return new NavigationBundleManeuver(
                "maneuver-" + type,
                null,
                null,
                null,
                null,
                null,
                type,
                sampleAction,
                10,
                20,
                List.of(),
                0,
                1,
                100d,
                100d,
                0d,
                100d
        );
    }

    private static List<NavigationManeuver> maneuversForTypes(int... types) {
        List<NavigationManeuver> maneuvers = new ArrayList<>(types.length);
        for (int index = 0; index < types.length; index++) {
            maneuvers.add(maneuver(index, index + 1, types[index]));
        }
        return List.copyOf(maneuvers);
    }

    private static NavigationManeuver maneuver(int beginShapeIndex, int endShapeIndex, int type) {
        return new NavigationManeuver(
                beginShapeIndex,
                endShapeIndex,
                "maneuver-" + type,
                null,
                null,
                null,
                null,
                null,
                type,
                null,
                null,
                List.of()
        );
    }

    private static List<SnappedRoutePoint> shapePoints(int count) {
        List<SnappedRoutePoint> shape = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            shape.add(new SnappedRoutePoint(37.5665 + (index * 0.0005), 126.9780 + (index * 0.0005)));
        }
        return List.copyOf(shape);
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
