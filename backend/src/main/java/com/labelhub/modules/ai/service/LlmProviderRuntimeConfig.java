package com.labelhub.modules.ai.service;

import java.util.Map;

public record LlmProviderRuntimeConfig(
        String baseUrl,
        String apiKey,
        String modelName,
        Map<String, String> customHeaders,
        ProviderCapability capability
) {
    public LlmProviderRuntimeConfig(String baseUrl, String apiKey, String modelName,
                                    Map<String, String> customHeaders) {
        this(baseUrl, apiKey, modelName, customHeaders, ProviderCapability.textOnly());
    }
}
