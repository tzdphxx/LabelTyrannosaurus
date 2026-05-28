package com.labelhub.infrastructure.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.modules.ai.dto.LlmProviderTestResponse;
import com.labelhub.modules.ai.service.LlmProviderRuntimeConfig;
import com.labelhub.modules.ai.service.LlmProviderTester;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OpenAiCompatibleProviderTester implements LlmProviderTester {

    private static final int MAX_RESPONSE_MESSAGE_LENGTH = 180;

    private final OpenAiCompatibleAdapter adapter;

    public OpenAiCompatibleProviderTester(OpenAiCompatibleAdapter adapter) {
        this.adapter = adapter;
    }

    OpenAiCompatibleProviderTester(ObjectMapper objectMapper, Duration timeout) {
        this(new OpenAiCompatibleAdapter(objectMapper, timeout));
    }

    @Override
    public LlmProviderTestResponse test(LlmProviderRuntimeConfig config) {
        OpenAiCompatibleResponse response = adapter.chat(config, List.of(new LlmMessage("user", "ping")), 1);
        if (response.success()) {
            return new LlmProviderTestResponse(true, response.latencyMs(), "OK");
        }
        return new LlmProviderTestResponse(false, response.latencyMs(), sanitize(response.errorMessage(), config.apiKey()));
    }

    private String sanitize(String message, String apiKey) {
        String sanitized = message == null ? "Provider test failed" : message;
        if (apiKey != null && !apiKey.isBlank()) {
            sanitized = sanitized.replace(apiKey, "***");
        }
        if (sanitized.length() > MAX_RESPONSE_MESSAGE_LENGTH) {
            return sanitized.substring(0, MAX_RESPONSE_MESSAGE_LENGTH);
        }
        return sanitized;
    }
}
