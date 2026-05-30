package com.labelhub.infrastructure.llm;

import java.util.Map;

public record LlmGatewayResponse(
        LlmGatewayStatus status,
        String rawResponse,
        String contentText,
        Map<String, Object> structuredJson,
        Long latencyMs,
        String errorCode,
        String errorMessage
) {
}
