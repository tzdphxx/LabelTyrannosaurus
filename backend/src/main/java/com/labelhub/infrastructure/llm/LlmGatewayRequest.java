package com.labelhub.infrastructure.llm;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "LLM 网关请求")
public record LlmGatewayRequest(
        @Schema(description = "大模型供应商 ID", example = "1")
        Long providerId,
        @Schema(description = "模型名称", example = "gpt-4o")
        String modelName,
        @Schema(description = "对话消息列表")
        List<LlmMessage> messages,
        @Schema(description = "响应格式约束，null 表示默认文本输出")
        ResponseFormat responseFormat
) {

    public LlmGatewayRequest(Long providerId, String modelName, List<LlmMessage> messages) {
        this(providerId, modelName, messages, null);
    }
}
