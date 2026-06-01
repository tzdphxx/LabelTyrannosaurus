package com.labelhub.modules.agent.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class AgentRunSnapshotRedactor {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private static final String REDACTED = "***REDACTED***";
    private static final Set<String> SENSITIVE_EXACT_KEYS = Set.of(
            "apikey", "api_key", "authorization", "bearer", "secret",
            "password", "providerkey", "provider_key", "headers",
            "requestheaders", "responseheaders", "token");
    private static final Set<String> SENSITIVE_SUFFIX_KEYS = Set.of(
            "token", "secret", "key", "password");
    private static final Set<String> LABELER_BLOCKED_KEYS = Set.of(
            "prompt", "messages", "input", "output", "rawresponse", "raw_response",
            "requestheaders", "responseheaders");

    private final ObjectMapper objectMapper;

    public AgentRunSnapshotRedactor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> full(String json) {
        Map<String, Object> snapshot = parse(json);
        return snapshot == null ? null : redactMap(snapshot, false);
    }

    public Map<String, Object> labelerSummary(String json) {
        Map<String, Object> snapshot = parse(json);
        return snapshot == null ? null : redactMap(snapshot, true);
    }

    private Map<String, Object> parse(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception e) {
            return Map.of("raw", json);
        }
    }

    private Map<String, Object> redactMap(Map<String, Object> source, boolean labelerSummary) {
        Map<String, Object> redacted = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            String normalized = normalize(key);
            if (labelerSummary && LABELER_BLOCKED_KEYS.contains(normalized)) {
                return;
            }
            if (isSensitiveKey(normalized)) {
                redacted.put(key, REDACTED);
                return;
            }
            redacted.put(key, redactValue(value, labelerSummary));
        });
        return redacted;
    }

    private Object redactValue(Object value, boolean labelerSummary) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> typed = new LinkedHashMap<>();
            map.forEach((key, nestedValue) -> typed.put(String.valueOf(key), nestedValue));
            return redactMap(typed, labelerSummary);
        }
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(item -> redactValue(item, labelerSummary))
                    .toList();
        }
        return value;
    }

    private boolean isSensitiveKey(String normalized) {
        if (SENSITIVE_EXACT_KEYS.contains(normalized)) {
            return true;
        }
        return SENSITIVE_SUFFIX_KEYS.stream().anyMatch(suffix ->
                normalized.endsWith(suffix) && normalized.length() > suffix.length());
    }

    private String normalize(String key) {
        return key == null ? "" : key.replace("-", "").toLowerCase(Locale.ROOT);
    }
}
