package com.labelhub.common.api;

public record ApiResponse<T>(int code, String message, T data, String traceId) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(0, "OK", data, null);
    }

    public static <T> ApiResponse<T> fail(int code, String message, String traceId) {
        return new ApiResponse<>(code, message, null, traceId);
    }
}
