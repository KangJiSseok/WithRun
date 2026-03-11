package kr.withrun.was.domain.course.vo;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.withrun.was.global.exception.CustomException;
import kr.withrun.was.global.response.ResponseCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("좌표 값 객체")
class CoordinatesTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @DisplayName("직접 생성 시 고도 정보가 없으면 예외를 던진다")
    @Test
    void rejectsMissingElevationsOnConstruction() {
        assertThatThrownBy(() -> new Coordinates(
                List.of(37.5665),
                List.of(126.9780),
                null
        ))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.MISSING_COURSE_COORDINATE_ELEVATION);
    }

    @DisplayName("객체 형태 JSON에 고도 목록이 없으면 예외를 던진다")
    @Test
    void rejectsObjectJsonWithoutElevations() throws Exception {
        assertThatThrownBy(() -> Coordinates.fromJson(objectMapper.readTree("""
                {
                  "latitudes": [37.5665],
                  "longitudes": [126.9780]
                }
                """)))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.MISSING_COURSE_COORDINATE_ELEVATION);
    }

    @DisplayName("배열 형태 좌표에 고도 값이 하나라도 없으면 예외를 던진다")
    @Test
    void rejectsPointSequenceWithoutElevation() throws Exception {
        assertThatThrownBy(() -> Coordinates.fromJson(objectMapper.readTree("""
                [
                  [37.5665, 126.9780],
                  [37.5668, 126.9791, 15.5]
                ]
                """)))
                .isInstanceOf(CustomException.class)
                .extracting("responseCode")
                .isEqualTo(ResponseCode.MISSING_COURSE_COORDINATE_ELEVATION);
    }

    @DisplayName("배열 형태 좌표가 위도 경도 고도를 모두 가지면 역직렬화한다")
    @Test
    void deserializesPointSequenceWithElevations() throws Exception {
        Coordinates coordinates = Coordinates.fromJson(objectMapper.readTree("""
                [
                  [37.5665, 126.9780, 12.0],
                  [37.5668, 126.9791, 15.5]
                ]
                """));

        assertThat(coordinates).isEqualTo(new Coordinates(
                List.of(37.5665, 37.5668),
                List.of(126.9780, 126.9791),
                List.of(12.0, 15.5)
        ));
    }
}
