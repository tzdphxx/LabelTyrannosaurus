package com.labelhub.modules.ai.dto;

import com.labelhub.infrastructure.llm.LlmGatewayStatus;
import java.util.Map;

public record AiReviewPromptTestResponse(
        Long agentRunId,
        LlmGatewayStatus status,
        String contentText,
        Map<String, Object> structuredJson,
        String rawResponse,
        Long latencyMs,
        String errorCode,
        String errorMessage
) {
}
