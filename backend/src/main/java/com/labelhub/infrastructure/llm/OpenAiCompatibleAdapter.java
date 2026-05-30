package com.labelhub.infrastructure.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.modules.ai.service.LlmProviderRuntimeConfig;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OpenAiCompatibleAdapter {

    private static final String CHAT_COMPLETIONS_PATH = "/chat/completions";
    private static final int MAX_RESPONSE_MESSAGE_LENGTH = 180;

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Duration timeout;
    private final Duration visionTimeout;

    @Autowired
    public OpenAiCompatibleAdapter(ObjectMapper objectMapper,
                                   @Value("${labelhub.llm.gateway.timeout-ms:30000}") long timeoutMs,
                                   @Value("${labelhub.llm.gateway.vision-timeout-ms:60000}") long visionTimeoutMs) {
        this(objectMapper, Duration.ofMillis(timeoutMs), Duration.ofMillis(visionTimeoutMs));
    }

    OpenAiCompatibleAdapter(ObjectMapper objectMapper, Duration timeout) {
        this(objectMapper, timeout, timeout);
    }

    OpenAiCompatibleAdapter(ObjectMapper objectMapper, Duration timeout, Duration visionTimeout) {
        this.objectMapper = objectMapper;
        this.timeout = timeout;
        this.visionTimeout = visionTimeout;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout.compareTo(visionTimeout) >= 0 ? timeout : visionTimeout)
                .build();
    }

    public OpenAiCompatibleResponse chat(LlmProviderRuntimeConfig config, List<LlmMessage> messages) {
        return chat(config, messages, null, null);
    }

    public OpenAiCompatibleResponse chat(LlmProviderRuntimeConfig config, List<LlmMessage> messages, Integer maxTokens) {
        return chat(config, messages, maxTokens, null);
    }

    public OpenAiCompatibleResponse chat(LlmProviderRuntimeConfig config, List<LlmMessage> messages,
                                         Integer maxTokens, List<ToolDefinition> tools) {
        Instant startedAt = Instant.now();
        try {
            HttpRequest request = buildRequest(config, messages, maxTokens, tools);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long latencyMs = Duration.between(startedAt, Instant.now()).toMillis();
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return OpenAiCompatibleResponse.success(response.statusCode(), response.body(), latencyMs);
            }
            return OpenAiCompatibleResponse.failure(response.statusCode(), response.body(), latencyMs,
                    sanitize("Provider call failed with status " + response.statusCode() + ": " + response.body(),
                            config.apiKey()),
                    false);
        } catch (HttpConnectTimeoutException ex) {
            return failed(startedAt, "Provider call timed out", config.apiKey(), true);
        } catch (IOException ex) {
            return failed(startedAt, "Provider call I/O failure: " + ex.getMessage(), config.apiKey(), isTimeout(ex));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return failed(startedAt, "Provider call was interrupted", config.apiKey(), false);
        } catch (RuntimeException ex) {
            return failed(startedAt, "Provider call failed: " + ex.getMessage(), config.apiKey(), false);
        }
    }

    private HttpRequest buildRequest(LlmProviderRuntimeConfig config, List<LlmMessage> messages,
                                     Integer maxTokens, List<ToolDefinition> tools)
            throws JsonProcessingException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(config.baseUrl() + CHAT_COMPLETIONS_PATH))
                .timeout(containsImagePart(messages) ? visionTimeout : timeout)
                .header("Content-Type", "application/json");
        if (config.customHeaders() != null) {
            config.customHeaders().forEach(builder::header);
        }
        if (config.apiKey() != null && !config.apiKey().isBlank()) {
            builder.setHeader("Authorization", "Bearer " + config.apiKey());
        }
        return builder.POST(HttpRequest.BodyPublishers.ofString(requestBody(config.modelName(), messages, maxTokens, tools))).build();
    }

    private String requestBody(String modelName, List<LlmMessage> messages, Integer maxTokens,
                               List<ToolDefinition> tools) throws JsonProcessingException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", modelName);
        payload.put("messages", serializeMessages(messages));
        if (maxTokens != null) {
            payload.put("max_tokens", maxTokens);
        }
        if (tools != null && !tools.isEmpty()) {
            payload.put("tools", tools);
        }
        payload.put("temperature", 0);
        return objectMapper.writeValueAsString(payload);
    }

    private List<Map<String, Object>> serializeMessages(List<LlmMessage> messages) {
        if (messages == null) {
            return List.of();
        }
        return messages.stream().map(this::serializeMessage).toList();
    }

    private Map<String, Object> serializeMessage(LlmMessage msg) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("role", msg.role());
        if (msg.contentParts() != null && !msg.contentParts().isEmpty()) {
            map.put("content", msg.contentParts().stream().map(this::serializeContentPart).toList());
        } else if (msg.content() != null) {
            map.put("content", msg.content());
        }
        if (msg.toolCalls() != null && !msg.toolCalls().isEmpty()) {
            map.put("tool_calls", msg.toolCalls().stream().map(tc -> {
                Map<String, Object> tcMap = new LinkedHashMap<>();
                tcMap.put("id", tc.id());
                tcMap.put("type", tc.type());
                tcMap.put("function", Map.of("name", tc.function().name(), "arguments", tc.function().arguments()));
                return tcMap;
            }).toList());
        }
        if (msg.toolCallId() != null) {
            map.put("tool_call_id", msg.toolCallId());
        }
        if (msg.name() != null) {
            map.put("name", msg.name());
        }
        return map;
    }

    private Map<String, Object> serializeContentPart(LlmMessage.ContentPart part) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (part instanceof LlmMessage.TextPart textPart) {
            map.put("type", "text");
            map.put("text", textPart.text());
            return map;
        }
        if (part instanceof LlmMessage.ImageUrlPart imageUrlPart) {
            map.put("type", "image_url");
            Map<String, Object> imageUrl = new LinkedHashMap<>();
            imageUrl.put("url", imageUrlPart.url());
            if (imageUrlPart.detail() != null && !imageUrlPart.detail().isBlank()) {
                imageUrl.put("detail", imageUrlPart.detail());
            }
            map.put("image_url", imageUrl);
            return map;
        }
        throw new IllegalArgumentException("Unsupported content part: " + part);
    }

    private boolean containsImagePart(List<LlmMessage> messages) {
        if (messages == null) {
            return false;
        }
        return messages.stream()
                .filter(message -> message.contentParts() != null)
                .flatMap(message -> message.contentParts().stream())
                .anyMatch(LlmMessage.ImageUrlPart.class::isInstance);
    }

    private OpenAiCompatibleResponse failed(Instant startedAt, String message, String apiKey, boolean timedOut) {
        long latencyMs = Duration.between(startedAt, Instant.now()).toMillis();
        return OpenAiCompatibleResponse.failure(null, null, latencyMs, sanitize(message, apiKey), timedOut);
    }

    private boolean isTimeout(IOException ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof TimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private String sanitize(String message, String apiKey) {
        String sanitized = message == null ? "Provider call failed" : message;
        if (apiKey != null && !apiKey.isBlank()) {
            sanitized = sanitized.replace(apiKey, "***");
        }
        if (sanitized.length() > MAX_RESPONSE_MESSAGE_LENGTH) {
            return sanitized.substring(0, MAX_RESPONSE_MESSAGE_LENGTH);
        }
        return sanitized;
    }
}
