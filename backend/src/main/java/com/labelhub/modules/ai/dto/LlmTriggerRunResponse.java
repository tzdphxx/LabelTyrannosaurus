package com.labelhub.modules.ai.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import com.labelhub.infrastructure.llm.LlmGatewayStatus;

public record LlmTriggerRunResponse(
        Long triggerRunId,
        Long agentRunId,
        Long componentId,
        Map<String, Object> suggestionJson,
        Map<String, Object> patch,
        String displayText,
        List<String> targetFields,
        String rawModelSummary,
        BigDecimal confidence,
        List<String> warnings,
        String traceId,
        Object status,
        Long latencyMs,
        String errorCode,
        String errorMessage
) {
    public LlmTriggerRunResponse(Long agentRunId,
                                 Map<String, Object> suggestionJson,
                                 String displayText,
                                 List<String> targetFields,
                                 String rawModelSummary,
                                 LlmGatewayStatus status,
                                 Long latencyMs,
                                 String errorCode,
                                 String errorMessage) {
        this(null, agentRunId, null, suggestionJson, suggestionJson, displayText, targetFields,
                rawModelSummary, null, List.of(), null, status, latencyMs, errorCode, errorMessage);
    }
}
