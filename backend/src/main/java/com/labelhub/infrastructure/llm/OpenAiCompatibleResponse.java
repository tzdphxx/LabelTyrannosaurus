package com.labelhub.infrastructure.llm;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "OpenAI 兼容接口响应")
public record OpenAiCompatibleResponse(
        @Schema(description = "是否调用成功")
        boolean success,
        @Schema(description = "HTTP 状态码", example = "200")
        Integer httpStatus,
        @Schema(description = "原始响应文本")
        String rawResponse,
        @Schema(description = "调用耗时（毫秒）", example = "1200")
        Long latencyMs,
        @Schema(description = "错误详情")
        String errorMessage,
        @Schema(description = "是否超时")
        boolean timedOut
) {

    public static OpenAiCompatibleResponse success(Integer httpStatus, String rawResponse, Long latencyMs) {
        return new OpenAiCompatibleResponse(true, httpStatus, rawResponse, latencyMs, null, false);
    }

    public static OpenAiCompatibleResponse failure(Integer httpStatus,
                                                   String rawResponse,
                                                   Long latencyMs,
                                                   String errorMessage,
                                                   boolean timedOut) {
        return new OpenAiCompatibleResponse(false, httpStatus, rawResponse, latencyMs, errorMessage, timedOut);
    }
}
