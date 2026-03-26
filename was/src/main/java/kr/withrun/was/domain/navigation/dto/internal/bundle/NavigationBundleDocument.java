package kr.withrun.was.domain.navigation.dto.internal.bundle;

import kr.withrun.was.domain.navigation.dto.internal.valhalla.MatchedRoutePoint;
import kr.withrun.was.domain.navigation.dto.internal.valhalla.RouteEdge;
import kr.withrun.was.domain.navigation.dto.internal.valhalla.SnappedRoutePoint;

import java.util.List;

/**
 * S3 에 저장되는 latest navigation bundle 문서 구조를 표현하는 내부 DTO 입니다.
 */
public record NavigationBundleDocument(
        List<SnappedRoutePoint> shape,
        List<NavigationBundleManeuver> maneuvers,
        NavigationBundleMetadata metadata,
        List<RouteEdge> edges,
        List<MatchedRoutePoint> matchedPoints,
        List<Double> shapeLengths
) {
}
