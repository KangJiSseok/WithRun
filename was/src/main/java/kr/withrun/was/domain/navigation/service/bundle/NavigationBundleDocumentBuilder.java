package kr.withrun.was.domain.navigation.service.bundle;

import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleDocument;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleManeuver;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleMetadata;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleManeuverSampleAction;
import kr.withrun.was.domain.navigation.dto.internal.bundle.NavigationBundleVoiceInstruction;
import kr.withrun.was.domain.navigation.dto.internal.valhalla.NavigationManeuver;
import kr.withrun.was.domain.navigation.dto.internal.valhalla.ValhallaVoiceInstruction;
import kr.withrun.was.domain.navigation.dto.internal.valhalla.ValhallaRouteTraceResult;
import kr.withrun.was.domain.navigation.dto.internal.valhalla.ValhallaTraceAttributes;
import kr.withrun.was.domain.navigation.exception.NavigationBundleGenerationException;
import kr.withrun.was.domain.navigation.type.NavigationBundleFailureCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Component
/**
 * Valhalla 응답을 latest navigation bundle 문서 구조로 조립하는 빌더입니다.
 */
public class NavigationBundleDocumentBuilder {

    private final ShapeDistanceCalculator shapeDistanceCalculator;
    private final Clock clock;
    private final String valhallaVersion;
    private final String defaultLanguage;

    @Autowired
    public NavigationBundleDocumentBuilder(
            ShapeDistanceCalculator shapeDistanceCalculator,
            @Value("${valhalla.version:unknown}") String valhallaVersion,
            @Value("${app.navigation.language:ko-KR}") String defaultLanguage
    ) {
        this(shapeDistanceCalculator, Clock.systemUTC(), valhallaVersion, defaultLanguage);
    }

    NavigationBundleDocumentBuilder(
            Clock clock,
            String valhallaVersion,
            String defaultLanguage
    ) {
        this(new ShapeDistanceCalculator(), clock, valhallaVersion, defaultLanguage);
    }

    NavigationBundleDocumentBuilder(
            ShapeDistanceCalculator shapeDistanceCalculator,
            Clock clock,
            String valhallaVersion,
            String defaultLanguage
    ) {
        this.shapeDistanceCalculator = shapeDistanceCalculator;
        this.clock = clock;
        this.valhallaVersion = valhallaVersion;
        this.defaultLanguage = defaultLanguage;
    }

    /**
     * 코스와 Valhalla 결과를 합쳐 저장 가능한 번들 문서를 생성합니다.
     */
    public NavigationBundleDocument build(
            Course course,
            ValhallaRouteTraceResult routeTraceResult,
            ValhallaTraceAttributes traceAttributes
    ) {
        List<Double> cumulativeDistances = shapeDistanceCalculator.cumulativeDistancesMeters(routeTraceResult.shape());
        Instant generatedAt = Instant.now(clock);

        return new NavigationBundleDocument(
                routeTraceResult.shape(),
                normalizeManeuvers(routeTraceResult.maneuvers(), cumulativeDistances),
                new NavigationBundleMetadata(
                        cumulativeDistances.isEmpty() ? 0d : cumulativeDistances.get(cumulativeDistances.size() - 1),
                        generatedAt,
                        valhallaVersion,
                        defaultLanguage
                ),
                traceAttributes.edges(),
                traceAttributes.matchedPoints(),
                traceAttributes.shapeLengths()
        );
    }

    private List<NavigationBundleManeuver> normalizeManeuvers(
            List<NavigationManeuver> maneuvers,
            List<Double> cumulativeDistances
    ) {
        if (maneuvers == null || maneuvers.isEmpty()) {
            return List.of();
        }

        List<NavigationBundleManeuver> normalizedManeuvers = new ArrayList<>(maneuvers.size());
        for (NavigationManeuver maneuver : maneuvers) {
            validateShapeIndex(maneuver.beginShapeIndex(), cumulativeDistances.size(), "begin");
            validateShapeIndex(maneuver.endShapeIndex(), cumulativeDistances.size(), "end");
            if (maneuver.beginShapeIndex() > maneuver.endShapeIndex()) {
                throw new NavigationBundleGenerationException(
                        NavigationBundleFailureCode.BUNDLE_VALIDATION_FAILED,
                        "Maneuver beginShapeIndex must be less than or equal to endShapeIndex"
                );
            }

            double startDistanceMeters = cumulativeDistances.get(maneuver.beginShapeIndex());
            double endDistanceMeters = cumulativeDistances.get(maneuver.endShapeIndex());
            ManeuverMetadata maneuverMetadata = classifyManeuver(maneuver.type());
            normalizedManeuvers.add(new NavigationBundleManeuver(
                    maneuver.instruction(),
                    maneuver.verbalTransitionAlertInstruction(),
                    maneuver.verbalPreTransitionInstruction(),
                    maneuver.verbalPostTransitionInstruction(),
                    maneuver.verbalMultiCue(),
                    maneuver.verbalSuccinctTransitionInstruction(),
                    maneuver.type(),
                    maneuverMetadata.sampleAction(),
                    maneuver.bearingBefore(),
                    maneuver.bearingAfter(),
                    normalizeVoiceInstructions(maneuver.voiceInstructions()),
                    maneuver.beginShapeIndex(),
                    maneuver.endShapeIndex(),
                    Math.max(0d, endDistanceMeters - startDistanceMeters),
                    endDistanceMeters,
                    startDistanceMeters,
                    endDistanceMeters
            ));
        }

        return List.copyOf(normalizedManeuvers);
    }

    private ManeuverMetadata classifyManeuver(Integer type) {
        if (type == null) {
            return ManeuverMetadata.empty();
        }

        return switch (type) {
            case 0, 1, 2, 3, 7, 8, 17, 22, 25, 26, 27, 28, 29, 30, 31, 32,
                    33, 34, 35, 36, 39, 40, 41, 42, 43, 44, 45 -> new ManeuverMetadata(NavigationBundleManeuverSampleAction.STRAIGHT);
            case 4, 5, 6 -> new ManeuverMetadata(NavigationBundleManeuverSampleAction.ARRIVAL);
            case 9, 10, 11, 18, 20, 23, 37 -> new ManeuverMetadata(NavigationBundleManeuverSampleAction.RIGHT);
            case 12, 13 -> new ManeuverMetadata(NavigationBundleManeuverSampleAction.UTURN);
            case 14, 15, 16, 19, 21, 24, 38 -> new ManeuverMetadata(NavigationBundleManeuverSampleAction.LEFT);
            default -> new ManeuverMetadata(NavigationBundleManeuverSampleAction.STRAIGHT);
        };
    }

    private List<NavigationBundleVoiceInstruction> normalizeVoiceInstructions(
            List<ValhallaVoiceInstruction> voiceInstructions
    ) {
        if (voiceInstructions == null || voiceInstructions.isEmpty()) {
            return List.of();
        }

        List<NavigationBundleVoiceInstruction> normalizedVoiceInstructions = new ArrayList<>(voiceInstructions.size());
        for (ValhallaVoiceInstruction voiceInstruction : voiceInstructions) {
            normalizedVoiceInstructions.add(new NavigationBundleVoiceInstruction(
                    voiceInstruction.distanceAlongGeometry(),
                    voiceInstruction.announcement(),
                    voiceInstruction.ssmlAnnouncement()
            ));
        }
        return List.copyOf(normalizedVoiceInstructions);
    }

    private void validateShapeIndex(int shapeIndex, int shapeSize, String fieldName) {
        if (shapeIndex < 0 || shapeIndex >= shapeSize) {
            throw new NavigationBundleGenerationException(
                    NavigationBundleFailureCode.BUNDLE_VALIDATION_FAILED,
                    "Maneuver " + fieldName + "ShapeIndex is out of range"
            );
        }
    }

    private record ManeuverMetadata(NavigationBundleManeuverSampleAction sampleAction) {

        private static ManeuverMetadata empty() {
            return new ManeuverMetadata(null);
        }
    }
}
