package com.labelhub.common.exception;

import com.labelhub.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex,
                                                                      HttpServletRequest request) {
        return ResponseEntity.status(statusFor(ex.getCode()))
                .body(ApiResponse.fail(ex.getCode(), ex.getMessage(), traceId(request)));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex,
                                                                       HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(this::validationMessage)
                .orElse("请求参数不合法");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(400102, message, traceId(request)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception, traceId={}", traceId(request), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(500001, "系统异常，请稍后重试", traceId(request)));
    }

    private String validationMessage(FieldError error) {
        String defaultMessage = error.getDefaultMessage();
        if (defaultMessage == null || defaultMessage.isBlank()) {
            return error.getField() + " 参数不合法";
        }
        return error.getField() + "：" + validationReason(defaultMessage);
    }

    private String validationReason(String defaultMessage) {
        return switch (defaultMessage) {
            case "must not be blank", "must not be empty", "must not be null" -> "必填参数不能为空";
            case "must be a well-formed email address" -> "邮箱格式不正确";
            default -> defaultMessage;
        };
    }

    private HttpStatus statusFor(int code) {
        return switch (code) {
            case 401001 -> HttpStatus.UNAUTHORIZED;
            case 403001 -> HttpStatus.FORBIDDEN;
            case 409101, 409201, 409301 -> HttpStatus.CONFLICT;
            case 429001 -> HttpStatus.TOO_MANY_REQUESTS;
            case 500001 -> HttpStatus.INTERNAL_SERVER_ERROR;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    private String traceId(HttpServletRequest request) {
        return request.getHeader("X-Trace-Id");
    }
}
