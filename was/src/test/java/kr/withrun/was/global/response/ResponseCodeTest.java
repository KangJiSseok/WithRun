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

    @DisplayName("코스 없음 코드를 Not Found 실패로 노출한다")
    @Test
    void exposesCourseNotFoundAsNotFoundFailure() {
        ApiResponse<Void> response = ApiResponse.fail(ResponseCode.COURSE_NOT_FOUND);

        assertThat(ResponseCode.COURSE_NOT_FOUND.getCode()).isEqualTo("E304");
        assertThat(ResponseCode.COURSE_NOT_FOUND.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(ResponseCode.COURSE_NOT_FOUND.getMessage()).isEqualTo("코스를 찾을 수 없습니다.");
        assertThat(ResponseCode.COURSE_NOT_FOUND.isSuccess()).isFalse();
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getCode()).isEqualTo("E304");
        assertThat(response.getMessage()).isEqualTo("코스를 찾을 수 없습니다.");
    }

    @DisplayName("러닝 세션 오류 코드를 기대한 상태와 메시지로 노출한다")
    @Test
    void exposesRunningSessionErrorCodes() {
        assertNotFoundResponseCode(ResponseCode.RUNNING_SESSION_NOT_FOUND, "E400", "러닝 세션을 찾을 수 없습니다.");

        ApiResponse<Void> conflictResponse = ApiResponse.fail(ResponseCode.RUNNING_SESSION_ALREADY_COMPLETED);

        assertThat(ResponseCode.RUNNING_SESSION_ALREADY_COMPLETED.getCode()).isEqualTo("E401");
        assertThat(ResponseCode.RUNNING_SESSION_ALREADY_COMPLETED.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(ResponseCode.RUNNING_SESSION_ALREADY_COMPLETED.getMessage()).isEqualTo("이미 종료된 러닝 세션입니다.");
        assertThat(ResponseCode.RUNNING_SESSION_ALREADY_COMPLETED.isSuccess()).isFalse();
        assertThat(conflictResponse.isSuccess()).isFalse();
        assertThat(conflictResponse.getCode()).isEqualTo("E401");
        assertThat(conflictResponse.getMessage()).isEqualTo("이미 종료된 러닝 세션입니다.");
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

    private static void assertNotFoundResponseCode(ResponseCode responseCode, String code, String message) {
        ApiResponse<Void> response = ApiResponse.fail(responseCode);

        assertThat(responseCode.getCode()).isEqualTo(code);
        assertThat(responseCode.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(responseCode.getMessage()).isEqualTo(message);
        assertThat(responseCode.isSuccess()).isFalse();
        assertThat(response.isSuccess()).isFalse();
        assertThat(response.getCode()).isEqualTo(code);
        assertThat(response.getMessage()).isEqualTo(message);
    }
}
