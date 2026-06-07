package com.labelhub.modules.ai.dto;

public record LlmProviderTestResponse(Boolean success, Long latencyMs, String message) {
}
