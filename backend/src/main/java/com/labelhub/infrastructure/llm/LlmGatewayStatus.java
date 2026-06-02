package com.labelhub.infrastructure.llm;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "LLM 网关调用状态")
public enum LlmGatewayStatus {
    @Schema(description = "调用成功")
    SUCCESS,
    @Schema(description = "供应商不可用")
    PROVIDER_UNAVAILABLE,
    @Schema(description = "供应商内部错误")
    PROVIDER_ERROR,
    @Schema(description = "被限流")
    RATE_LIMITED,
    @Schema(description = "调用超时")
    TIMEOUT,
    @Schema(description = "返回无效 JSON")
    INVALID_JSON
}
