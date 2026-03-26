package kr.withrun.was.domain.navigation.service.input;

import kr.withrun.was.domain.navigation.dto.internal.valhalla.ValhallaRequestPoint;
import kr.withrun.was.domain.navigation.dto.internal.valhalla.ValhallaTraceRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 밀도가 높은 입력 shape 를 재시도용으로 재샘플링합니다.
 */
@Component
public class CourseTraceResampler {

    private static final double MIN_RESAMPLE_DISTANCE_METERS = 5d;
    private static final double EARTH_RADIUS_METERS = 6_371_000d;

    public ValhallaTraceRequest resample(ValhallaTraceRequest request) {
        if (request.shape().size() <= 2) {
            return request;
        }

        List<ValhallaRequestPoint> resampled = new ArrayList<>();
        resampled.add(request.shape().get(0));

        for (int index = 1; index < request.shape().size() - 1; index++) {
            ValhallaRequestPoint candidate = request.shape().get(index);
            ValhallaRequestPoint lastAccepted = resampled.get(resampled.size() - 1);
            if (distanceMeters(lastAccepted, candidate) >= MIN_RESAMPLE_DISTANCE_METERS) {
                resampled.add(candidate);
            }
        }

        ValhallaRequestPoint lastPoint = request.shape().get(request.shape().size() - 1);
        ValhallaRequestPoint lastAccepted = resampled.get(resampled.size() - 1);
        if (!isSamePoint(lastAccepted, lastPoint)) {
            resampled.add(lastPoint);
        }

        return request.withShape(resampled);
    }

    private boolean isSamePoint(ValhallaRequestPoint left, ValhallaRequestPoint right) {
        return Double.compare(left.lat(), right.lat()) == 0
                && Double.compare(left.lon(), right.lon()) == 0;
    }

    private double distanceMeters(ValhallaRequestPoint left, ValhallaRequestPoint right) {
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
