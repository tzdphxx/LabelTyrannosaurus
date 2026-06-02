package com.labelhub.modules.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Map;
import com.labelhub.infrastructure.llm.LlmGatewayStatus;

@Schema(description = "大模型触发运行响应")
public record LlmTriggerRunResponse(
        @Schema(description = "触发运行ID")
        Long triggerRunId,
        @Schema(description = "代理运行ID")
        Long agentRunId,
        @Schema(description = "组件ID")
        String componentId,
        @Schema(description = "建议JSON")
        Map<String, Object> suggestionJson,
        @Schema(description = "显示文本")
        String displayText,
        @Schema(description = "目标字段列表")
        List<String> targetFields,
        @Schema(description = "原始模型摘要")
        String rawModelSummary,
        @Schema(description = "运行状态")
        Object status,
        @Schema(description = "延迟毫秒数")
        Long latencyMs,
        @Schema(description = "错误码")
        String errorCode,
        @Schema(description = "错误消息")
        String errorMessage
) {
    public LlmTriggerRunResponse(Long agentRunId,
                                 String componentId,
                                 Map<String, Object> suggestionJson,
                                 String displayText,
                                 List<String> targetFields,
                                 String rawModelSummary,
                                 LlmGatewayStatus status,
                                 Long latencyMs,
                                 String errorCode,
                                 String errorMessage) {
        this(null, agentRunId, componentId, suggestionJson, displayText, targetFields,
                rawModelSummary, status, latencyMs, errorCode, errorMessage);
    }
}
