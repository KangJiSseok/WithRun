package kr.withrun.was.global.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("응답 코드")
class ResponseCodeTest {

    @DisplayName("주변 코스 잘못된 요청 코드를 Bad Request 실패로 노출한다")
    @Test
    void exposesInvalidNearbyCourseRequestCodesAsCommonBadRequestFailures() {
        assertResponseCode(ResponseCode.INVALID_RADIUS, "E006", "잘못된 반경 값입니다.");
        assertResponseCode(ResponseCode.INVALID_TARGET_DISTANCE, "E007", "잘못된 목표 거리 값입니다.");
        assertResponseCode(ResponseCode.INVALID_CURSOR, "E008", "잘못된 커서 값입니다.");
    }

    private static void assertResponseCode(ResponseCode responseCode, String code, String message) {
        ApiResponse<Void> response = ApiResponse.fail(responseCode);

        assertThat(responseCode.getCode()).isEqualTo(code);
        assertThat(responseCode.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(responseCode.getMessage()).isEqualTo(message);
        assertThat(responseCode.isSuccess()).isFalse();
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getCode()).isEqualTo(code);
        assertThat(response.getMessage()).isEqualTo(message);
    }
}
