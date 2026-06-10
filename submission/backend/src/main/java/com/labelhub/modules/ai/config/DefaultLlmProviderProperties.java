package com.labelhub.modules.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "labelhub.llm.default-provider")
public record DefaultLlmProviderProperties(
        String providerCode,
        String providerName,
        String baseUrl,
        String apiKey,
        String defaultModel) {

    public boolean hasRequiredRuntimeConfig() {
        return hasText(baseUrl) && hasText(apiKey) && hasText(defaultModel);
    }

    public String normalizedProviderCode() {
        return hasText(providerCode) ? providerCode.trim() : "dashscope-default";
    }

    public String normalizedProviderName() {
        return hasText(providerName) ? providerName.trim() : "DashScope Default";
    }

    public String normalizedBaseUrl() {
        return trimTrailingSlash(baseUrl.trim());
    }

    public String normalizedApiKey() {
        return apiKey.trim();
    }

    public String normalizedDefaultModel() {
        return defaultModel.trim();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String trimTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }
}
