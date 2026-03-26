package kr.withrun.was.domain.navigation.dto.internal.valhalla;

import java.util.List;

/**
 * Valhalla trace_attributes 응답의 도로 구간 정보를 표현합니다.
 */
public record RouteEdge(
        int beginShapeIndex,
        int endShapeIndex,
        double lengthKm,
        List<String> names
) {
}
