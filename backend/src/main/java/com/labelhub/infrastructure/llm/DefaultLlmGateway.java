package com.labelhub.infrastructure.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.modules.ai.service.LlmProviderRuntimeConfig;
import com.labelhub.modules.ai.service.LlmProviderService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DefaultLlmGateway implements LlmGateway {

    private static final Logger log = LoggerFactory.getLogger(DefaultLlmGateway.class);

    private static final String PROVIDER_UNAVAILABLE = "PROVIDER_UNAVAILABLE";
    private static final String PROVIDER_ERROR = "PROVIDER_ERROR";
    private static final String TIMEOUT = "TIMEOUT";
    private static final String INVALID_JSON = "INVALID_JSON";
    private static final int MAX_RETRY_CONTENT_LENGTH = 2000;
    private static final TypeReference<Map<String, Object>> OBJECT_MAP = new TypeReference<>() {
    };

    private final LlmProviderService llmProviderService;
    private final OpenAiCompatibleAdapter adapter;
    private final ObjectMapper objectMapper;
    private final AiMetrics aiMetrics;

    @Autowired
    public DefaultLlmGateway(LlmProviderService llmProviderService,
                             OpenAiCompatibleAdapter adapter,
                             ObjectMapper objectMapper,
                             AiMetrics aiMetrics) {
        this.llmProviderService = llmProviderService;
        this.adapter = adapter;
        this.objectMapper = objectMapper;
        this.aiMetrics = aiMetrics;
    }

    DefaultLlmGateway(LlmProviderService llmProviderService, OpenAiCompatibleAdapter adapter) {
        this(llmProviderService, adapter, new ObjectMapper(), null);
    }

    @Override
    public LlmGatewayResponse review(LlmGatewayRequest request) {
        Optional<LlmProviderRuntimeConfig> runtimeConfig =
                llmProviderService.findEnabledRuntimeConfig(request.providerId(), request.modelName());
        if (runtimeConfig.isEmpty()) {
            return recordAndReturn(request.providerId(), request.modelName(),
                    failure(LlmGatewayStatus.PROVIDER_UNAVAILABLE, null, null, null,
                            PROVIDER_UNAVAILABLE, "LLM provider is unavailable"));
        }
        LlmProviderRuntimeConfig config = selectRuntimeModel(runtimeConfig.get(), request.messages());
        ResponseFormat responseFormat = resolveResponseFormat(config, request.responseFormat());
        OpenAiCompatibleResponse adapterResponse = adapter.chat(config, request.messages(), null, null, responseFormat);
        if (adapterResponse.timedOut()) {
            return recordAndReturn(request.providerId(), config.modelName(),
                    failure(LlmGatewayStatus.TIMEOUT, adapterResponse.rawResponse(), null, adapterResponse.latencyMs(),
                            TIMEOUT, adapterResponse.errorMessage()));
        }
        if (!adapterResponse.success()) {
            return recordAndReturn(request.providerId(), config.modelName(),
                    failure(LlmGatewayStatus.PROVIDER_ERROR, adapterResponse.rawResponse(), null, adapterResponse.latencyMs(),
                    PROVIDER_ERROR, adapterResponse.errorMessage()));
        }
        LlmGatewayResponse parsed = extractStructuredJson(adapterResponse);
        if (parsed.status() == LlmGatewayStatus.INVALID_JSON && parsed.contentText() != null) {
            OpenAiCompatibleResponse retryResponse = adapter.chat(config,
                    messagesWithJsonRepairInstruction(request.messages(), parsed),
                    null, null, responseFormat);
            if (retryResponse.timedOut()) {
                return recordAndReturn(request.providerId(), config.modelName(),
                        failure(LlmGatewayStatus.TIMEOUT, retryResponse.rawResponse(), null, retryResponse.latencyMs(),
                                TIMEOUT, retryResponse.errorMessage()));
            }
            if (!retryResponse.success()) {
                return recordAndReturn(request.providerId(), config.modelName(),
                        failure(LlmGatewayStatus.PROVIDER_ERROR, retryResponse.rawResponse(), null, retryResponse.latencyMs(),
                                PROVIDER_ERROR, retryResponse.errorMessage()));
            }
            LlmGatewayResponse retryParsed = extractStructuredJson(retryResponse);
            if (retryParsed.status() == LlmGatewayStatus.INVALID_JSON && retryParsed.contentText() != null) {
                return recordAndReturn(request.providerId(), config.modelName(),
                        new LlmGatewayResponse(LlmGatewayStatus.SUCCESS, retryParsed.rawResponse(),
                                retryParsed.contentText(), Map.of(), retryParsed.latencyMs(), null, null));
            }
            return recordAndReturn(request.providerId(), config.modelName(), retryParsed);
        }
        return recordAndReturn(request.providerId(), config.modelName(), parsed);
    }

    private LlmProviderRuntimeConfig selectRuntimeModel(LlmProviderRuntimeConfig config, java.util.List<LlmMessage> messages) {
        if (!containsMediaPart(messages)
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

    private boolean containsMediaPart(java.util.List<LlmMessage> messages) {
        if (messages == null) {
            return false;
        }
        return messages.stream()
                .filter(message -> message.contentParts() != null)
                .flatMap(message -> message.contentParts().stream())
                .anyMatch(part -> part instanceof LlmMessage.ImageUrlPart
                        || part instanceof LlmMessage.VideoUrlPart);
    }

    /**
     * The provider's configured {@code structuredOutputMode} is the gate:
     * <ul>
     *   <li>NONE — never send response_format, regardless of what the caller requested</li>
     *   <li>JSON_OBJECT — always send json_object</li>
     *   <li>JSON_SCHEMA — send the caller's schema if supplied, otherwise fall back to json_object</li>
     * </ul>
     */
    private ResponseFormat resolveResponseFormat(LlmProviderRuntimeConfig config, ResponseFormat requested) {
        String mode = config.capability() == null ? "NONE" : config.capability().structuredOutputMode();
        if (mode == null || "NONE".equals(mode)) {
            return ResponseFormat.none();
        }
        if ("JSON_SCHEMA".equals(mode)) {
            if (requested != null && requested.mode() == ResponseFormat.Mode.JSON_SCHEMA
                    && requested.jsonSchema() != null) {
                return requested;
            }
            return ResponseFormat.jsonObject();
        }
        // JSON_OBJECT
        return ResponseFormat.jsonObject();
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

    private List<LlmMessage> messagesWithJsonRepairInstruction(List<LlmMessage> messages, LlmGatewayResponse parseFailure) {
        List<LlmMessage> retryMessages = new ArrayList<>(messages == null ? List.of() : messages);
        retryMessages.add(new LlmMessage("user", jsonRepairInstruction(parseFailure)));
        return retryMessages;
    }

    private String jsonRepairInstruction(LlmGatewayResponse parseFailure) {
        return """
                上一次模型输出无法解析为合法 JSON。
                解析错误：%s
                上一次输出：
                %s

                请重新生成结果。要求：
                1. 只返回一个合法 JSON 对象。
                2. 不要 Markdown 代码块。
                3. 不要解释文字。
                4. 字段名、字段类型必须符合本轮任务要求。"""
                .formatted(parseFailure.errorMessage(), truncate(parseFailure.contentText(), MAX_RETRY_CONTENT_LENGTH));
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String extractContentText(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.isNull()) {
                return null;
            }
            if (content.isArray()) {
                for (JsonNode part : content) {
                    if ("text".equals(part.path("type").asText()) && part.has("text")) {
                        return part.get("text").asText();
                    }
                }
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
        if (firstLineEnd < 0) {
            return trimmed;
        }
        int lastFenceStart = trimmed.lastIndexOf("\n```");
        if (lastFenceStart <= firstLineEnd) {
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

    private LlmGatewayResponse recordAndReturn(Long providerId, String modelName, LlmGatewayResponse response) {
        if (aiMetrics != null) {
            aiMetrics.record("LLM_GATEWAY", providerId, modelName,
                    response.status() == null ? "UNKNOWN" : response.status().name(),
                    response.errorCode(), response.latencyMs());
        } else {
            log.debug("AiMetrics not available; skipping LLM gateway metric recording");
        }
        return response;
    }
}
