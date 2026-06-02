package com.labelhub.common.api;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "统一 API 响应包装")
public record ApiResponse<T>(
        @Schema(description = "业务状态码，0 表示成功", example = "0")
        int code,
        @Schema(description = "响应消息", example = "OK")
        String message,
        @Schema(description = "响应数据负载")
        T data,
        @Schema(description = "请求追踪 ID，用于问题排查", example = "trace-abc123")
        String traceId
) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "OK", data, null);
    }

    public static <T> ApiResponse<T> fail(int code, String message, String traceId) {
        return new ApiResponse<>(code, message, null, traceId);
    }
}
