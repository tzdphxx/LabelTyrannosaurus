package com.labelhub.modules.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "大模型供应商测试响应")
public record LlmProviderTestResponse(
        @Schema(description = "测试是否成功")
        Boolean success,
        @Schema(description = "延迟毫秒数")
        Long latencyMs,
        @Schema(description = "测试结果消息")
        String message
) {
}
