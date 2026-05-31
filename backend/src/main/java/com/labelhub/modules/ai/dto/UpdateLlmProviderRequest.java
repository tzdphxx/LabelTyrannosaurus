package com.labelhub.modules.ai.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record UpdateLlmProviderRequest(
        @NotBlank @Size(max = 64) String providerCode,
        @NotBlank @Size(max = 100) String providerName,
        @NotBlank @Size(max = 500) String baseUrl,
        @Size(max = 4096) String apiKey,
        @NotBlank @Size(max = 128) String defaultModel,
        Map<String, String> customHeaders,
        @Min(0) Integer platformRateLimitPerMinute,
        @Min(0) Integer taskRateLimitPerMinute,
        @Min(0) Integer userRateLimitPerMinute
) {
}
