package kr.withrun.was.domain.navigation.dto.internal.valhalla;

import java.util.List;

/**
 * Valhalla trace API 호출에 필요한 입력 파라미터를 담는 내부 DTO 입니다.
 */
public record ValhallaTraceRequest(
        List<ValhallaRequestPoint> shape,
        String costing,
        String shapeMatch,
        int gpsAccuracy,
        int searchRadius,
        int turnPenaltyFactor,
        String language,
        String units
) {

    public ValhallaTraceRequest withTraceOptions(int updatedGpsAccuracy, int updatedSearchRadius) {
        return new ValhallaTraceRequest(
                shape,
                costing,
                shapeMatch,
                updatedGpsAccuracy,
                updatedSearchRadius,
                turnPenaltyFactor,
                language,
                units
        );
    }

    public ValhallaTraceRequest withShape(List<ValhallaRequestPoint> updatedShape) {
        return new ValhallaTraceRequest(
                List.copyOf(updatedShape),
                costing,
                shapeMatch,
                gpsAccuracy,
                searchRadius,
                turnPenaltyFactor,
                language,
                units
        );
    }
}
