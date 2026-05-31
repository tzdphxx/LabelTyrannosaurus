package com.labelhub.modules.ai.dto;

import java.time.LocalDateTime;
import java.util.Map;

public record LlmProviderResponse(
        Long id,
        String providerCode,
        String providerName,
        String baseUrl,
        String defaultModel,
        Map<String, String> customHeaders,
        Boolean enabled,
        Integer platformRateLimitPerMinute,
        Integer taskRateLimitPerMinute,
        Integer userRateLimitPerMinute,
        Boolean apiKeyConfigured,
        Long createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
