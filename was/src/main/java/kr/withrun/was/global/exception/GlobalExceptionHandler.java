package kr.withrun.was.global.exception;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import kr.withrun.was.global.response.ApiResponse;
import kr.withrun.was.global.response.ResponseCode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * CustomException 처리
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustomException(CustomException e) {
        log.warn("CustomException: {} - {}", e.getResponseCode().getCode(), e.getMessage());

        return ResponseEntity
                .status(e.getResponseCode().getStatus())
                .body(ApiResponse.fail(e.getResponseCode()));
    }

    /**
     * @Valid 검증 실패 처리
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<ValidationError>>> handleValidationException(
            MethodArgumentNotValidException e) {

        return buildValidationErrorResponse(e.getBindingResult(), "Validation failed");
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<List<ValidationError>>> handleBindException(BindException e) {
        return buildValidationErrorResponse(e.getBindingResult(), "Binding failed");
    }

    private ResponseEntity<ApiResponse<List<ValidationError>>> buildValidationErrorResponse(
            BindingResult bindingResult,
            String logPrefix
    ) {
        List<ValidationError> errors = extractValidationErrors(bindingResult);
        log.warn("{}: {}", logPrefix, errors);
        ResponseCode responseCode = resolveResponseCode(bindingResult);

        return ResponseEntity
                .status(responseCode.getStatus())
                .body(ApiResponse.fail(responseCode, errors));
    }

    /**
     * 예상치 못한 예외 처리
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception e) {
        CustomException customException = findCustomException(e);
        if (customException != null) {
            return handleCustomException(customException);
        }

        log.error("Unexpected exception: ", e);

        return ResponseEntity
                .internalServerError()
                .body(ApiResponse.fail(ResponseCode.INTERNAL_SERVER_ERROR));
    }

    private List<ValidationError> extractValidationErrors(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
                .map(error -> new ValidationError(
                        error.getField(),
                        error.getRejectedValue(),
                        error.getDefaultMessage()))
                .collect(Collectors.toList());
    }

    private ResponseCode resolveResponseCode(BindingResult bindingResult) {
        for (FieldError error : bindingResult.getFieldErrors()) {
            if (isSpecificNumericValidation(error, "radiusM")) {
                return ResponseCode.INVALID_RADIUS;
            }
            if (isSpecificNumericValidation(error, "targetDistanceM")) {
                return ResponseCode.INVALID_TARGET_DISTANCE;
            }
        }

        return ResponseCode.INVALID_INPUT_VALUE;
    }

    private boolean isSpecificNumericValidation(FieldError error, String fieldName) {
        if (!fieldName.equals(error.getField())) {
            return false;
        }

        Object rejectedValue = error.getRejectedValue();
        if (rejectedValue == null) {
            return false;
        }
        if (rejectedValue instanceof String stringValue && stringValue.isBlank()) {
            return false;
        }

        return true;
    }

    private CustomException findCustomException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof CustomException customException) {
                return customException;
            }
            current = current.getCause();
        }

        return null;
    }

    public record ValidationError(String field, Object rejectedValue, String message) {
    }
}
