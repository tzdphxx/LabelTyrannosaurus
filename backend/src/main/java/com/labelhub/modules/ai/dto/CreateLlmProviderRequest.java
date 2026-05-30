package com.labelhub.modules.ai.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record CreateLlmProviderRequest(
        @NotBlank @Size(max = 64) String providerCode,
        @NotBlank @Size(max = 100) String providerName,
        @NotBlank @Size(max = 500) String baseUrl,
        @NotBlank @Size(max = 4096) String apiKey,
        @NotBlank @Size(max = 128) String defaultModel,
        Map<String, String> customHeaders,
        @Min(0) Integer platformRateLimitPerMinute,
        @Min(0) Integer taskRateLimitPerMinute,
        @Min(0) Integer userRateLimitPerMinute,
        Boolean supportVision,
        Boolean supportMultiImage,
        @Min(0) Integer maxImageCount,
        @Size(max = 100) String visionModel
) {
    public CreateLlmProviderRequest(String providerCode, String providerName, String baseUrl, String apiKey,
                                    String defaultModel, Map<String, String> customHeaders,
                                    Integer platformRateLimitPerMinute, Integer taskRateLimitPerMinute,
                                    Integer userRateLimitPerMinute) {
        this(providerCode, providerName, baseUrl, apiKey, defaultModel, customHeaders,
                platformRateLimitPerMinute, taskRateLimitPerMinute, userRateLimitPerMinute,
                false, false, 10, null);
    }
}
