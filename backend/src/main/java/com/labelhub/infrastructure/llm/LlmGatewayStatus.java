package com.labelhub.infrastructure.llm;

public enum LlmGatewayStatus {
    SUCCESS,
    PROVIDER_UNAVAILABLE,
    PROVIDER_ERROR,
    RATE_LIMITED,
    TIMEOUT,
    INVALID_JSON
}
