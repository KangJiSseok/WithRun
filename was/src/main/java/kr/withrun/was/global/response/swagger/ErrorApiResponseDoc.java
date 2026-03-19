package kr.withrun.was.global.response.swagger;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ErrorApiResponse", description = "비즈니스 오류 또는 서버 오류 응답 래퍼")
public record ErrorApiResponseDoc(
        @Schema(description = "요청 성공 여부. 오류 응답은 항상 false 다.", example = "false")
        boolean success,

        @Schema(description = "오류 응답 코드", example = "E304")
        String code,

        @Schema(description = "오류 메시지", example = "코스를 찾을 수 없습니다.")
        String message,

        @Schema(description = "오류 추적용 식별자", example = "1a2b3c4d")
        String traceId
) {
}
