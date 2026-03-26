package kr.withrun.was.domain.navigation.dto.internal.bundle;

import java.util.List;

/**
 * 클라이언트용 번들에 저장되는 maneuver 정규화 결과입니다.
 */
public record NavigationBundleManeuver(
        String instruction,
        String verbalTransitionAlertInstruction,
        String verbalPreTransitionInstruction,
        String verbalPostTransitionInstruction,
        Boolean verbalMultiCue,
        String verbalSuccinctTransitionInstruction,
        Integer type,
        NavigationBundleManeuverSampleAction sampleAction,
        Integer bearingBefore,
        Integer bearingAfter,
        List<NavigationBundleVoiceInstruction> voiceInstructions,
        int beginShapeIndex,
        int endShapeIndex,
        double distanceMeters,
        double cumulativeDistanceMeters,
        double startDistanceMeters,
        double endDistanceMeters
) {
}
