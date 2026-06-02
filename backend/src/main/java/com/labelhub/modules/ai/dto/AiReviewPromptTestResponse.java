package com.labelhub.modules.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.labelhub.infrastructure.llm.LlmGatewayStatus;
import java.util.Map;

@Schema(description = "AI审核提示词测试响应")
public record AiReviewPromptTestResponse(
        @Schema(description = "代理运行ID")
        Long agentRunId,
        @Schema(description = "运行状态")
        LlmGatewayStatus status,
        @Schema(description = "模型返回的文本内容")
        String contentText,
        @Schema(description = "结构化JSON结果")
        Map<String, Object> structuredJson,
        @Schema(description = "原始响应文本")
        String rawResponse,
        @Schema(description = "延迟毫秒数")
        Long latencyMs,
        @Schema(description = "错误码")
        String errorCode,
        @Schema(description = "错误消息")
        String errorMessage
) {
}
