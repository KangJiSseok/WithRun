package kr.withrun.was.domain.navigation.service.input;

import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.vo.GeoPoint;
import kr.withrun.was.domain.navigation.dto.internal.valhalla.ValhallaRequestPoint;
import kr.withrun.was.domain.navigation.dto.internal.valhalla.ValhallaTraceRequest;
import kr.withrun.was.domain.navigation.exception.NavigationBundleGenerationException;
import kr.withrun.was.domain.navigation.type.NavigationBundleFailureCode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
/**
 * 코스 좌표를 Valhalla 호출에 맞는 shape 명령으로 정규화하는 컴포넌트입니다.
 */
public class CourseTraceRequestMapper {

    private static final String DEFAULT_COSTING = "pedestrian";
    private static final String DEFAULT_SHAPE_MATCH = "map_snap";
    private static final int DEFAULT_GPS_ACCURACY = 5;
    private static final int DEFAULT_SEARCH_RADIUS = 20;
    private static final int DEFAULT_TURN_PENALTY_FACTOR = 500;
    private static final String DEFAULT_LANGUAGE = "ko-KR";
    private static final String DEFAULT_UNITS = "kilometers";
    private static final double MIN_DEDUPLICATION_DISTANCE_METERS = 2d;
    private static final double EARTH_RADIUS_METERS = 6_371_000d;

    /**
     * 코스 좌표를 검증하고 연속된 중복점을 제거한 뒤 Valhalla 명령 객체로 변환합니다.
     */
    public ValhallaTraceRequest map(Course course) {
        List<GeoPoint> coursePoints = course.getCoordinates() == null ? List.of() : course.getCoordinates().values();
        List<ValhallaRequestPoint> tracePoints = new ArrayList<>();

        for (GeoPoint coursePoint : coursePoints) {
            if (coursePoint == null || coursePoint.latitude() == null || coursePoint.longitude() == null) {
                continue;
            }

            ValhallaRequestPoint currentPoint = new ValhallaRequestPoint(coursePoint.latitude(), coursePoint.longitude());
            if (!tracePoints.isEmpty()) {
                ValhallaRequestPoint lastPoint = tracePoints.get(tracePoints.size() - 1);
                if (distanceMeters(lastPoint, currentPoint) < MIN_DEDUPLICATION_DISTANCE_METERS) {
                    continue;
                }
            }

            tracePoints.add(currentPoint);
        }

        if (tracePoints.size() < 2) {
            throw new NavigationBundleGenerationException(
                    NavigationBundleFailureCode.INVALID_COURSE_COORDINATES,
                    "At least two distinct course coordinates are required"
            );
        }

        return new ValhallaTraceRequest(
                List.copyOf(tracePoints),
                DEFAULT_COSTING,
                DEFAULT_SHAPE_MATCH,
                DEFAULT_GPS_ACCURACY,
                DEFAULT_SEARCH_RADIUS,
                DEFAULT_TURN_PENALTY_FACTOR,
                DEFAULT_LANGUAGE,
                DEFAULT_UNITS
        );
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
