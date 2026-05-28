package com.labelhub.infrastructure.llm;

public enum LlmGatewayStatus {
    SUCCESS,
    PROVIDER_UNAVAILABLE,
    PROVIDER_ERROR,
    TIMEOUT,
    INVALID_JSON
}
