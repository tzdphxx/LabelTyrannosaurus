package com.labelhub.infrastructure.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.modules.ai.service.LlmProviderRuntimeConfig;
import com.labelhub.modules.ai.service.LlmProviderService;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DefaultLlmGateway implements LlmGateway {

    private static final String PROVIDER_UNAVAILABLE = "PROVIDER_UNAVAILABLE";
    private static final String PROVIDER_ERROR = "PROVIDER_ERROR";
    private static final String TIMEOUT = "TIMEOUT";
    private static final String INVALID_JSON = "INVALID_JSON";
    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };

    private final LlmProviderService llmProviderService;
    private final OpenAiCompatibleAdapter adapter;
    private final ObjectMapper objectMapper;

    @Autowired
    public DefaultLlmGateway(LlmProviderService llmProviderService,
                             OpenAiCompatibleAdapter adapter,
                             ObjectMapper objectMapper) {
        this.llmProviderService = llmProviderService;
        this.adapter = adapter;
        this.objectMapper = objectMapper;
    }

    DefaultLlmGateway(LlmProviderService llmProviderService, OpenAiCompatibleAdapter adapter) {
        this(llmProviderService, adapter, new ObjectMapper());
    }

    @Override
    public LlmGatewayResponse review(LlmGatewayRequest request) {
        Optional<LlmProviderRuntimeConfig> runtimeConfig =
                llmProviderService.findEnabledRuntimeConfig(request.providerId(), request.modelName());
        if (runtimeConfig.isEmpty()) {
            return failure(LlmGatewayStatus.PROVIDER_UNAVAILABLE, null, null, null,
                    PROVIDER_UNAVAILABLE, "LLM provider is unavailable");
        }
        LlmProviderRuntimeConfig config = selectRuntimeModel(runtimeConfig.get(), request.messages());
        OpenAiCompatibleResponse adapterResponse = adapter.chat(config, request.messages());
        if (adapterResponse.timedOut()) {
            return failure(LlmGatewayStatus.TIMEOUT, adapterResponse.rawResponse(), null, adapterResponse.latencyMs(),
                    TIMEOUT, adapterResponse.errorMessage());
        }
        if (!adapterResponse.success()) {
            return failure(LlmGatewayStatus.PROVIDER_ERROR, adapterResponse.rawResponse(), null, adapterResponse.latencyMs(),
                    PROVIDER_ERROR, adapterResponse.errorMessage());
        }
        return extractStructuredJson(adapterResponse);
    }

    private LlmProviderRuntimeConfig selectRuntimeModel(LlmProviderRuntimeConfig config, java.util.List<LlmMessage> messages) {
        if (!containsImagePart(messages)
                || config.capability() == null
                || config.capability().visionModel() == null
                || config.capability().visionModel().isBlank()) {
            return config;
        }
        return new LlmProviderRuntimeConfig(
                config.baseUrl(),
                config.apiKey(),
                config.capability().visionModel(),
                config.customHeaders(),
                config.capability());
    }

    private boolean containsImagePart(java.util.List<LlmMessage> messages) {
        if (messages == null) {
            return false;
        }
        return messages.stream()
                .filter(message -> message.contentParts() != null)
                .flatMap(message -> message.contentParts().stream())
                .anyMatch(LlmMessage.ImageUrlPart.class::isInstance);
    }

    private LlmGatewayResponse extractStructuredJson(OpenAiCompatibleResponse adapterResponse) {
        String contentText = extractContentText(adapterResponse.rawResponse());
        if (contentText == null) {
            return failure(LlmGatewayStatus.INVALID_JSON, adapterResponse.rawResponse(), null, adapterResponse.latencyMs(),
                    INVALID_JSON, "Provider response did not contain message content");
        }
        try {
            Map<String, Object> structuredJson = objectMapper.readValue(stripJsonFence(contentText), OBJECT_MAP);
            return new LlmGatewayResponse(LlmGatewayStatus.SUCCESS, adapterResponse.rawResponse(), contentText,
                    structuredJson, adapterResponse.latencyMs(), null, null);
        } catch (JsonProcessingException ex) {
            return failure(LlmGatewayStatus.INVALID_JSON, adapterResponse.rawResponse(), contentText,
                    adapterResponse.latencyMs(), INVALID_JSON, "Model output is not valid JSON");
        }
    }

    private String extractContentText(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.isNull()) {
                return null;
            }
            return content.asText();
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private String stripJsonFence(String contentText) {
        String trimmed = contentText.trim();
        if (!trimmed.startsWith("```")) {
            return trimmed;
        }
        int firstLineEnd = trimmed.indexOf('\n');
        int lastFenceStart = trimmed.lastIndexOf("```");
        if (firstLineEnd < 0 || lastFenceStart <= firstLineEnd) {
            return trimmed;
        }
        return trimmed.substring(firstLineEnd + 1, lastFenceStart).trim();
    }

    private LlmGatewayResponse failure(LlmGatewayStatus status,
                                       String rawResponse,
                                       String contentText,
                                       Long latencyMs,
                                       String errorCode,
                                       String errorMessage) {
        return new LlmGatewayResponse(status, rawResponse, contentText, null, latencyMs, errorCode, errorMessage);
    }
}
