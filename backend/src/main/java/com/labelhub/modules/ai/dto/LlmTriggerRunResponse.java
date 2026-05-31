package com.labelhub.modules.ai.dto;

import com.labelhub.infrastructure.llm.LlmGatewayStatus;
import java.util.List;
import java.util.Map;

public record LlmTriggerRunResponse(
        Long agentRunId,
        String componentId,
        Map<String, Object> suggestionJson,
        String displayText,
        List<String> targetFields,
        String rawModelSummary,
        LlmGatewayStatus status,
        Long latencyMs,
        String errorCode,
        String errorMessage
) {
}
