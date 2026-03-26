package kr.withrun.was.domain.navigation.dto.internal.valhalla;

/**
 * Valhalla 가 보정한 최종 경로상의 좌표 한 점을 표현합니다.
 */
public record SnappedRoutePoint(
        double lat,
        double lon
) {
}
