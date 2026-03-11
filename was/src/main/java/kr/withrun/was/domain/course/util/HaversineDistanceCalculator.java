package kr.withrun.was.domain.course.util;

import kr.withrun.was.domain.course.vo.Coordinates;

import java.util.List;
import java.util.OptionalInt;

public final class HaversineDistanceCalculator {

    private static final double EARTH_RADIUS_M = 6_371_000D;

    private HaversineDistanceCalculator() {
    }

    public static OptionalInt calculateMinimumDistanceMeters(
            Coordinates coordinates,
            double userLatitude,
            double userLongitude
    ) {
        if (coordinates == null) {
            return OptionalInt.empty();
        }

        List<Double> latitudes = coordinates.latitudes();
        List<Double> longitudes = coordinates.longitudes();
        if (latitudes == null || longitudes == null || latitudes.size() != longitudes.size()) {
            return OptionalInt.empty();
        }

        Integer minimumDistance = null;
        for (int index = 0; index < latitudes.size(); index++) {
            Double latitude = latitudes.get(index);
            Double longitude = longitudes.get(index);
            if (latitude == null || longitude == null) {
                continue;
            }

            int distance = roundToNearestMeter(haversineDistanceMeters(userLatitude, userLongitude, latitude, longitude));
            if (minimumDistance == null || distance < minimumDistance) {
                minimumDistance = distance;
            }
        }

        if (minimumDistance == null) {
            return OptionalInt.empty();
        }

        return OptionalInt.of(minimumDistance);
    }

    private static double haversineDistanceMeters(
            double startLatitude,
            double startLongitude,
            double endLatitude,
            double endLongitude
    ) {
        double latitudeDelta = Math.toRadians(endLatitude - startLatitude);
        double longitudeDelta = Math.toRadians(endLongitude - startLongitude);
        double startLatitudeRadians = Math.toRadians(startLatitude);
        double endLatitudeRadians = Math.toRadians(endLatitude);

        double a = Math.pow(Math.sin(latitudeDelta / 2), 2)
                + Math.cos(startLatitudeRadians) * Math.cos(endLatitudeRadians) * Math.pow(Math.sin(longitudeDelta / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_M * c;
    }

    private static int roundToNearestMeter(double distanceMeters) {
        return (int) Math.round(distanceMeters);
    }
}
