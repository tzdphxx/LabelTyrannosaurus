package com.labelhub.modules.ai.dto;

import java.util.List;
import java.util.Map;
import com.labelhub.infrastructure.llm.LlmGatewayStatus;

public record LlmTriggerRunResponse(
        Long triggerRunId,
        Long agentRunId,
        String componentId,
        Map<String, Object> suggestionJson,
        String displayText,
        List<String> targetFields,
        String rawModelSummary,
        Object status,
        Long latencyMs,
        String errorCode,
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
