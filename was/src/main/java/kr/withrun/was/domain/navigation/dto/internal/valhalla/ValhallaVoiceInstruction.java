package kr.withrun.was.domain.navigation.dto.internal.valhalla;

/**
 * Valhalla maneuver 에 포함되는 음성 안내 항목입니다.
 */
public record ValhallaVoiceInstruction(
        double distanceAlongGeometry,
        String announcement,
        String ssmlAnnouncement
) {
}
