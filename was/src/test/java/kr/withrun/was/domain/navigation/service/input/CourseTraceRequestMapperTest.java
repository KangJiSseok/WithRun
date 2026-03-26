package kr.withrun.was.domain.navigation.service.input;

import kr.withrun.was.domain.course.entity.Course;
import kr.withrun.was.domain.course.type.CourseStatus;
import kr.withrun.was.domain.course.vo.Coordinates;
import kr.withrun.was.domain.navigation.dto.internal.valhalla.ValhallaTraceRequest;
import kr.withrun.was.domain.navigation.exception.NavigationBundleGenerationException;
import kr.withrun.was.domain.navigation.type.NavigationBundleFailureCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

@DisplayName("코스 네비게이션 입력 정규화")
class CourseTraceRequestMapperTest {

    private final CourseTraceRequestMapper mapper = new CourseTraceRequestMapper();

    @DisplayName("인접 중복 좌표를 제거하고 Valhalla 명령으로 정규화한다")
    @Test
    void mapsCourseCoordinatesIntoTraceRequest() {
        Course course = Course.builder()
                .title("Han River Loop")
                .status(CourseStatus.OFFICIAL)
                .distanceM(5000)
                .elevationGainM(30)
                .startLatitude(37.5665)
                .startLongitude(126.9780)
                .endLatitude(37.5701)
                .endLongitude(126.9824)
                .coordinates(new Coordinates(List.of(
                        point(37.5665, 126.9780, 12.0),
                        point(37.5665, 126.9780, 12.3),
                        point(37.5671, 126.9792, 13.0),
                        point(37.5701, 126.9824, 15.0)
                )))
                .build();

        ValhallaTraceRequest command = mapper.map(course);

        assertThat(command.shape()).extracting("lat", "lon")
                .containsExactly(
                        tuple(37.5665, 126.9780),
                        tuple(37.5671, 126.9792),
                        tuple(37.5701, 126.9824)
                );
        assertThat(command.costing()).isEqualTo("pedestrian");
        assertThat(command.shapeMatch()).isEqualTo("map_snap");
        assertThat(command.gpsAccuracy()).isEqualTo(5);
        assertThat(command.searchRadius()).isEqualTo(20);
        assertThat(command.turnPenaltyFactor()).isEqualTo(500);
        assertThat(command.language()).isEqualTo("ko-KR");
        assertThat(command.units()).isEqualTo("kilometers");
    }

    @DisplayName("null 이거나 좌표가 비어 있는 포인트는 제거하고 순서를 유지한다")
    @Test
    void removesInvalidPointsWhenEnoughValidPointsRemain() {
        Course course = Course.builder()
                .title("Noisy Route")
                .status(CourseStatus.OFFICIAL)
                .distanceM(3200)
                .elevationGainM(22)
                .startLatitude(37.5665)
                .startLongitude(126.9780)
                .endLatitude(37.5701)
                .endLongitude(126.9824)
                .coordinates(new Coordinates(List.of(
                        point(37.5665, 126.9780, 12.0),
                        new kr.withrun.was.domain.course.vo.GeoPoint(null, 126.9782, 12.1),
                        point(37.5665001, 126.9780001, 12.2),
                        new kr.withrun.was.domain.course.vo.GeoPoint(37.5674, null, 12.3),
                        point(37.5671, 126.9792, 13.0),
                        point(37.5701, 126.9824, 15.0)
                )))
                .build();

        ValhallaTraceRequest command = mapper.map(course);

        assertThat(command.shape()).extracting("lat", "lon")
                .containsExactly(
                        tuple(37.5665, 126.9780),
                        tuple(37.5671, 126.9792),
                        tuple(37.5701, 126.9824)
                );
    }

    @DisplayName("서로 다른 좌표가 2개 미만이면 실패한다")
    @Test
    void rejectsCourseWithLessThanTwoDistinctPoints() {
        Course course = Course.builder()
                .title("Invalid")
                .status(CourseStatus.OFFICIAL)
                .distanceM(1000)
                .elevationGainM(10)
                .startLatitude(37.5665)
                .startLongitude(126.9780)
                .coordinates(new Coordinates(List.of(
                        point(37.5665, 126.9780, 12.0),
                        point(37.5665, 126.9780, 13.0)
                )))
                .build();

        assertThatThrownBy(() -> mapper.map(course))
                .isInstanceOf(NavigationBundleGenerationException.class)
                .extracting("failureCode")
                .isEqualTo(NavigationBundleFailureCode.INVALID_COURSE_COORDINATES);
    }

    private static kr.withrun.was.domain.course.vo.GeoPoint point(double latitude, double longitude, double elevationM) {
        return new kr.withrun.was.domain.course.vo.GeoPoint(latitude, longitude, elevationM);
    }
}
