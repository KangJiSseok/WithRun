package kr.withrun.was.global.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private static final String TRACE_ID_KEY = "traceId";

    private final boolean success;
    private final String code;
    private final String message;
    private final T data;
    private final String traceId;

    @Builder
    private ApiResponse(boolean success, String code, String message, T data, String traceId) {
        this.success = success;
        this.code = code;
        this.message = message;
        this.data = data;
        // 성공 응답은 traceId 미포함, 실패 응답(4xx/5xx)만 traceId 포함
        this.traceId = success ? null : (traceId != null ? traceId : getOrCreateTraceId());
    }

    // 성공 응답 - 데이터만
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code(ResponseCode.OK.getCode())
                .message(ResponseCode.OK.getMessage())
                .data(data)
                .build();
    }

    // 성공 응답 - ResponseCode + 데이터
    public static <T> ApiResponse<T> success(ResponseCode code, T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .code(code.getCode())
                .message(code.getMessage())
                .data(data)
                .build();
    }

    // 성공 응답 - 데이터 없음
    public static <T> ApiResponse<T> success(ResponseCode code) {
        return ApiResponse.<T>builder()
                .success(true)
                .code(code.getCode())
                .message(code.getMessage())
                .build();
    }

    public static <T> ResponseEntity<ApiResponse<T>> successEntity(T data) {
        return ResponseEntity.ok(success(data));
    }

    public static <T> ResponseEntity<ApiResponse<T>> successEntity(ResponseCode code, T data) {
        return ResponseEntity.status(code.getStatus()).body(success(code, data));
    }

    public static ResponseEntity<ApiResponse<Void>> successEntity(ResponseCode code) {
        return ResponseEntity.status(code.getStatus()).body(success(code));
    }

    // 실패 응답
    public static <T> ApiResponse<T> fail(ResponseCode errorCode) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
    }

    // 실패 응답 - 데이터 포함 (유효성 검증 오류 등)
    public static <T> ApiResponse<T> fail(ResponseCode errorCode, T data) {
        return ApiResponse.<T>builder()
                .success(false)
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .data(data)
                .build();
    }

    /**
     * MDC에서 traceId를 가져오거나, 없으면 새로 생성합니다.
     * MDC에 traceId가 있으면 (Filter에서 설정한) 그 값을 사용하고,
     * 없으면 (테스트 등 비 HTTP 요청) 새로 생성합니다.
     */
    private String getOrCreateTraceId() {
        String traceId = MDC.get(TRACE_ID_KEY);
        if (traceId != null && !traceId.isEmpty()) {
            return traceId;
        }
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
