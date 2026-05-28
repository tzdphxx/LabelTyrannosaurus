package com.labelhub.common.exception;

import com.labelhub.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
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
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.fail(400102, "Invalid request parameter", traceId(request)));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(500001, "System error", traceId(request)));
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
