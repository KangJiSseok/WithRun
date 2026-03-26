package kr.withrun.was.domain.navigation.dto.internal.valhalla;

import java.util.List;

/**
 * trace_route 호출 결과에서 경로 좌표와 안내 정보를 모은 내부 DTO 입니다.
 */
public record ValhallaRouteTraceResult(
        List<SnappedRoutePoint> shape,
        List<NavigationManeuver> maneuvers
) {
}
