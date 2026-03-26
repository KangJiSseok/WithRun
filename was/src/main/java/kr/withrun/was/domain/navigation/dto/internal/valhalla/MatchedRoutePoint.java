package kr.withrun.was.domain.navigation.dto.internal.valhalla;

/**
 * Valhalla 가 원본 좌표를 도로망에 매칭한 결과 포인트를 표현합니다.
 */
public record MatchedRoutePoint(
        double lat,
        double lon,
        String type,
        Integer edgeIndex
) {
}
