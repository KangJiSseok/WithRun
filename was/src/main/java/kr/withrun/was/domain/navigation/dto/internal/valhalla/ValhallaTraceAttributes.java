package kr.withrun.was.domain.navigation.dto.internal.valhalla;

import java.util.List;

/**
 * trace_attributes 호출 결과에서 필요한 속성 정보만 모은 내부 DTO 입니다.
 */
public record ValhallaTraceAttributes(
        List<RouteEdge> edges,
        List<MatchedRoutePoint> matchedPoints,
        List<Double> shapeLengths
) {
}
