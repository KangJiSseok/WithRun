package kr.withrun.was.global.response;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ResponseCode {

    // ========== Success ==========
    OK("S001", HttpStatus.OK, "요청이 성공했습니다.", true),
    CREATED("S002", HttpStatus.CREATED, "리소스가 생성되었습니다.", true),
    NO_CONTENT("S003", HttpStatus.NO_CONTENT, "요청이 성공했습니다.", true),

    // User Success
    USER_REGISTERED("S100", HttpStatus.CREATED, "회원가입이 완료되었습니다.", true),
    USER_LOGGED_IN("S101", HttpStatus.OK, "로그인이 완료되었습니다.", true),
    USER_LOGGED_OUT("S102", HttpStatus.OK, "로그아웃이 완료되었습니다.", true),

    // ========== Error ==========
    // Common Error
    INVALID_INPUT_VALUE("E001", HttpStatus.BAD_REQUEST, "잘못된 입력값입니다.", false),
    METHOD_NOT_ALLOWED("E002", HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다.", false),
    ENTITY_NOT_FOUND("E003", HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다.", false),
    INTERNAL_SERVER_ERROR("E004", HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.", false),
    INVALID_TYPE_VALUE("E005", HttpStatus.BAD_REQUEST, "잘못된 타입입니다.", false),
    INVALID_RADIUS("E006", HttpStatus.BAD_REQUEST, "잘못된 반경 값입니다.", false),
    INVALID_TARGET_DISTANCE("E007", HttpStatus.BAD_REQUEST, "잘못된 목표 거리 값입니다.", false),
    INVALID_CURSOR("E008", HttpStatus.BAD_REQUEST, "잘못된 커서 값입니다.", false),

    // Course Error
    INVALID_COURSE_COORDINATES_FORMAT("E300", HttpStatus.BAD_REQUEST, "코스 좌표 형식이 올바르지 않습니다.", false),
    INVALID_COURSE_COORDINATES_LENGTH("E301", HttpStatus.BAD_REQUEST, "코스 좌표 목록 길이가 올바르지 않습니다.", false),
    INVALID_COURSE_COORDINATE_VALUE("E302", HttpStatus.BAD_REQUEST, "코스 좌표 값이 올바르지 않습니다.", false),
    MISSING_COURSE_COORDINATE_ELEVATION("E303", HttpStatus.BAD_REQUEST, "코스 좌표의 고도 정보가 필요합니다.", false),

    // Auth Error
    UNAUTHORIZED("E100", HttpStatus.UNAUTHORIZED, "인증이 필요합니다.", false),
    TOKEN_EXPIRED("E101", HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다.", false),
    TOKEN_INVALID("E102", HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다.", false),
    ACCESS_DENIED("E103", HttpStatus.FORBIDDEN, "접근 권한이 없습니다.", false),

    // User Error
    USER_NOT_FOUND("E200", HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다.", false),
    USER_ALREADY_EXISTS("E201", HttpStatus.CONFLICT, "이미 존재하는 사용자입니다.", false),
    PASSWORD_MISMATCH("E202", HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다.", false);

    private final String code;
    private final HttpStatus status;
    private final String message;
    private final boolean success;

    ResponseCode(String code, HttpStatus status, String message, boolean success) {
        this.code = code;
        this.status = status;
        this.message = message;
        this.success = success;
    }
}
