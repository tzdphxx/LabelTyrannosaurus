package com.labelhub.common.exception;

import com.labelhub.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void returnsOriginalChineseBusinessExceptionMessage() {
        HttpServletRequest request = requestWithTraceId("trace-1");

        ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessException(
                new BusinessException(403001, "当前账号没有权限执行该操作"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("当前账号没有权限执行该操作");
        assertThat(response.getBody().traceId()).isEqualTo("trace-1");
    }

    @Test
    void returnsChineseValidationMessage() throws Exception {
        HttpServletRequest request = requestWithTraceId("trace-2");
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(new FieldError("request", "username", "must not be blank"));

        ResponseEntity<ApiResponse<Void>> response = handler.handleValidationException(
                new MethodArgumentNotValidException(handlerMethodParameter(), bindingResult), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("username：必填参数不能为空");
    }

    @Test
    void returnsChineseSystemErrorMessage() {
        HttpServletRequest request = requestWithTraceId("trace-3");

        ResponseEntity<ApiResponse<Void>> response = handler.handleException(new RuntimeException("boom"), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("系统异常，请稍后重试");
    }

    private HttpServletRequest requestWithTraceId(String traceId) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Trace-Id")).thenReturn(traceId);
        return request;
    }

    private org.springframework.core.MethodParameter handlerMethodParameter() throws Exception {
        Method method = TestController.class.getDeclaredMethod("handle", Object.class);
        return new HandlerMethod(new TestController(), method).getMethodParameters()[0];
    }

    private static class TestController {
        @SuppressWarnings("unused")
        void handle(Object request) {
        }
    }
}
