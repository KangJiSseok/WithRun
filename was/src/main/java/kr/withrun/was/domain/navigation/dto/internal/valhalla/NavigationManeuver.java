package kr.withrun.was.domain.navigation.dto.internal.valhalla;

import java.util.List;

/**
 * 경로 안내 문구와 해당 shape 구간 범위를 표현합니다.
 */
public record NavigationManeuver(
        int beginShapeIndex,
        int endShapeIndex,
        String instruction,
        String verbalTransitionAlertInstruction,
        String verbalPreTransitionInstruction,
        String verbalPostTransitionInstruction,
        Boolean verbalMultiCue,
        String verbalSuccinctTransitionInstruction,
        Integer type,
        Integer bearingBefore,
        Integer bearingAfter,
        List<ValhallaVoiceInstruction> voiceInstructions
) {
}
