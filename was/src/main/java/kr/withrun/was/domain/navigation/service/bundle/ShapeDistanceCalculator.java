package kr.withrun.was.domain.navigation.service.bundle;

import kr.withrun.was.domain.navigation.dto.internal.valhalla.SnappedRoutePoint;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 스냅된 shape 의 누적 거리를 계산합니다.
 */
@Component
public class ShapeDistanceCalculator {

    private static final double EARTH_RADIUS_METERS = 6_371_000d;

    public List<Double> cumulativeDistancesMeters(List<SnappedRoutePoint> shape) {
        if (shape == null || shape.isEmpty()) {
            return List.of();
        }

        List<Double> cumulativeDistances = new ArrayList<>(shape.size());
        cumulativeDistances.add(0d);

        double accumulatedDistance = 0d;
        for (int index = 1; index < shape.size(); index++) {
            accumulatedDistance += distanceMeters(shape.get(index - 1), shape.get(index));
            cumulativeDistances.add(accumulatedDistance);
        }

        return List.copyOf(cumulativeDistances);
    }

    private double distanceMeters(SnappedRoutePoint left, SnappedRoutePoint right) {
        double leftLatitudeRadians = Math.toRadians(left.lat());
        double rightLatitudeRadians = Math.toRadians(right.lat());
        double latitudeDelta = Math.toRadians(right.lat() - left.lat());
        double longitudeDelta = Math.toRadians(right.lon() - left.lon());

        double a = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
                + Math.cos(leftLatitudeRadians) * Math.cos(rightLatitudeRadians)
                * Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }
}
