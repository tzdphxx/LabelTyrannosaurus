package com.labelhub.modules.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.audit.AuditCommand;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.common.web.TraceIdProvider;
import com.labelhub.modules.ai.domain.LlmProvider;
import com.labelhub.modules.ai.dto.CreateLlmProviderRequest;
import com.labelhub.modules.ai.dto.LlmProviderResponse;
import com.labelhub.modules.ai.dto.LlmProviderTestResponse;
import com.labelhub.modules.ai.dto.TestLlmProviderRequest;
import com.labelhub.modules.ai.dto.UpdateLlmProviderRequest;
import com.labelhub.modules.ai.mapper.LlmProviderMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LlmProviderService {

    private static final int PROVIDER_NOT_FOUND = 404301;
    private static final int PROVIDER_HEADER_INVALID = 400301;
    private static final int PROVIDER_JSON_INVALID = 500303;
    private static final String BIZ_TYPE = "LLM_PROVIDER";
    private static final String USER_ACTOR_TYPE = "USER";
    private static final String MASKED = "******";
    private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {
    };

    private final LlmProviderMapper llmProviderMapper;
    private final LlmApiKeyEncryptor encryptor;
    private final LlmProviderTester llmProviderTester;
    private final AuditAppender auditAppender;
    private final TraceIdProvider traceIdProvider;
    private final ObjectMapper objectMapper;

    @Autowired
    public LlmProviderService(LlmProviderMapper llmProviderMapper,
                              LlmApiKeyEncryptor encryptor,
                              LlmProviderTester llmProviderTester,
                              AuditAppender auditAppender,
                              TraceIdProvider traceIdProvider) {
        this(llmProviderMapper, encryptor, llmProviderTester, auditAppender, traceIdProvider, new ObjectMapper());
    }

    LlmProviderService(LlmProviderMapper llmProviderMapper,
                       LlmApiKeyEncryptor encryptor,
                       LlmProviderTester llmProviderTester,
                       AuditAppender auditAppender,
                       TraceIdProvider traceIdProvider,
                       ObjectMapper objectMapper) {
        this.llmProviderMapper = llmProviderMapper;
        this.encryptor = encryptor;
        this.llmProviderTester = llmProviderTester;
        this.auditAppender = auditAppender;
        this.traceIdProvider = traceIdProvider;
        this.objectMapper = objectMapper;
    }

    public List<LlmProviderResponse> listEnabled() {
        return llmProviderMapper.selectList(new QueryWrapper<LlmProvider>()
                        .eq("enabled", true)
                        .orderByDesc("updated_at"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<LlmProviderResponse> listAllForAdmin() {
        return llmProviderMapper.selectList(new QueryWrapper<LlmProvider>()
                        .orderByDesc("updated_at"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public LlmProviderResponse create(Long actorId, CreateLlmProviderRequest request) {
        LlmProvider provider = new LlmProvider();
        applyProviderFields(provider, request.providerCode(), request.providerName(), request.baseUrl(),
                request.defaultModel(), request.customHeaders(), request.platformRateLimitPerMinute(),
                request.taskRateLimitPerMinute(), request.userRateLimitPerMinute(),
                request.supportVision(), request.supportMultiImage(), request.maxImageCount(), request.visionModel(),
                request.structuredOutputMode());
        provider.setEncryptedApiKey(encryptor.encrypt(request.apiKey()));
        provider.setEnabled(true);
        provider.setCreatedBy(actorId);
        llmProviderMapper.insert(provider);
        auditAppender.append(new AuditCommand(USER_ACTOR_TYPE, actorId, BIZ_TYPE, provider.getId(), "LLM_PROVIDER_CREATED",
                null, auditSnapshot(provider), traceIdProvider.currentTraceId(), null));
        return toResponse(provider);
    }

    @Transactional
    public LlmProviderResponse update(Long actorId, Long providerId, UpdateLlmProviderRequest request) {
        LlmProvider provider = loadProvider(providerId);
        Map<String, Object> beforeJson = auditSnapshot(provider);
        applyProviderFields(provider, request.providerCode(), request.providerName(), request.baseUrl(),
                request.defaultModel(), request.customHeaders(), request.platformRateLimitPerMinute(),
                request.taskRateLimitPerMinute(), request.userRateLimitPerMinute(),
                request.supportVision(), request.supportMultiImage(), request.maxImageCount(), request.visionModel(),
                request.structuredOutputMode());
        if (hasText(request.apiKey())) {
            provider.setEncryptedApiKey(encryptor.encrypt(request.apiKey().trim()));
        }
        llmProviderMapper.updateById(provider);
        auditAppender.append(new AuditCommand(USER_ACTOR_TYPE, actorId, BIZ_TYPE, provider.getId(), "LLM_PROVIDER_UPDATED",
                beforeJson, auditSnapshot(provider), traceIdProvider.currentTraceId(), null));
        return toResponse(provider);
    }

    @Transactional
    public LlmProviderResponse enable(Long actorId, Long providerId) {
        return setEnabled(actorId, providerId, true, "LLM_PROVIDER_ENABLED");
    }

    @Transactional
    public LlmProviderResponse disable(Long actorId, Long providerId) {
        return setEnabled(actorId, providerId, false, "LLM_PROVIDER_DISABLED");
    }

    public Optional<LlmProvider> findEnabledById(Long providerId) {
        LlmProvider provider = llmProviderMapper.selectById(providerId);
        if (provider == null || !Boolean.TRUE.equals(provider.getEnabled())) {
            return Optional.empty();
        }
        return Optional.of(provider);
    }

    public Optional<LlmProviderRuntimeConfig> findEnabledRuntimeConfig(Long providerId, String modelName) {
        return findEnabledById(providerId)
                .map(provider -> new LlmProviderRuntimeConfig(
                        provider.getBaseUrl(),
                        decryptStoredApiKey(provider.getEncryptedApiKey()),
                        hasText(modelName) ? modelName.trim() : provider.getDefaultModel(),
                        parseHeaders(provider.getCustomHeadersJson()),
                        capability(provider)
                ));
    }

    public LlmProviderTestResponse test(Long actorId, Long providerId, TestLlmProviderRequest request) {
        LlmProvider provider = loadProvider(providerId);
        Map<String, String> headers = parseHeaders(provider.getCustomHeadersJson());
        headers.putAll(normalizeHeaders(request.customHeaders()));
        String apiKey = hasText(request.apiKey())
                ? request.apiKey().trim()
                : decryptStoredApiKey(provider.getEncryptedApiKey());
        String modelName = hasText(request.modelName()) ? request.modelName().trim() : provider.getDefaultModel();
        return llmProviderTester.test(new LlmProviderRuntimeConfig(provider.getBaseUrl(), apiKey, modelName, headers));
    }

    private LlmProviderResponse setEnabled(Long actorId, Long providerId, boolean enabled, String action) {
        LlmProvider provider = loadProvider(providerId);
        Map<String, Object> beforeJson = auditSnapshot(provider);
        provider.setEnabled(enabled);
        llmProviderMapper.updateById(provider);
        auditAppender.append(new AuditCommand(USER_ACTOR_TYPE, actorId, BIZ_TYPE, provider.getId(), action,
                beforeJson, auditSnapshot(provider), traceIdProvider.currentTraceId(), null));
        return toResponse(provider);
    }

    private void applyProviderFields(LlmProvider provider,
                                     String providerCode,
                                     String providerName,
                                     String baseUrl,
                                     String defaultModel,
                                     Map<String, String> customHeaders,
                                     Integer platformRateLimitPerMinute,
                                     Integer taskRateLimitPerMinute,
                                     Integer userRateLimitPerMinute,
                                     Boolean supportVision,
                                     Boolean supportMultiImage,
                                     Integer maxImageCount,
                                     String visionModel,
                                     String structuredOutputMode) {
        provider.setProviderCode(providerCode.trim());
        provider.setProviderName(providerName.trim());
        provider.setBaseUrl(trimTrailingSlash(baseUrl.trim()));
        provider.setDefaultModel(defaultModel.trim());
        provider.setCustomHeadersJson(toJson(normalizeHeaders(customHeaders)));
        provider.setPlatformRateLimitPerMinute(platformRateLimitPerMinute);
        provider.setTaskRateLimitPerMinute(taskRateLimitPerMinute);
        provider.setUserRateLimitPerMinute(userRateLimitPerMinute);
        provider.setSupportVision(Boolean.TRUE.equals(supportVision));
        provider.setSupportMultiImage(Boolean.TRUE.equals(supportMultiImage));
        provider.setMaxImageCount(maxImageCount != null ? maxImageCount : 10);
        provider.setVisionModel(hasText(visionModel) ? visionModel.trim() : null);
        provider.setStructuredOutputMode(normalizeStructuredOutputMode(structuredOutputMode));
    }

    private String normalizeStructuredOutputMode(String mode) {
        if (!hasText(mode)) {
            return null;
        }
        String normalized = mode.trim().toUpperCase(java.util.Locale.ROOT);
        return switch (normalized) {
            case "JSON_OBJECT", "JSON_SCHEMA", "NONE" -> normalized;
            default -> null;
        };
    }

    private LlmProvider loadProvider(Long providerId) {
        LlmProvider provider = llmProviderMapper.selectById(providerId);
        if (provider == null) {
            throw new BusinessException(PROVIDER_NOT_FOUND, "LLM Provider 不存在");
        }
        return provider;
    }

    private LlmProviderResponse toResponse(LlmProvider provider) {
        return new LlmProviderResponse(
                provider.getId(),
                provider.getProviderCode(),
                provider.getProviderName(),
                provider.getBaseUrl(),
                provider.getDefaultModel(),
                maskSensitiveHeaders(parseHeaders(provider.getCustomHeadersJson())),
                provider.getEnabled(),
                provider.getPlatformRateLimitPerMinute(),
                provider.getTaskRateLimitPerMinute(),
                provider.getUserRateLimitPerMinute(),
                provider.getSupportVision(),
                provider.getSupportMultiImage(),
                provider.getMaxImageCount(),
                provider.getVisionModel(),
                provider.getStructuredOutputMode(),
                hasText(provider.getEncryptedApiKey()),
                provider.getCreatedBy(),
                provider.getCreatedAt(),
                provider.getUpdatedAt()
        );
    }

    private Map<String, Object> auditSnapshot(LlmProvider provider) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", provider.getId());
        snapshot.put("providerCode", provider.getProviderCode());
        snapshot.put("providerName", provider.getProviderName());
        snapshot.put("baseUrl", provider.getBaseUrl());
        snapshot.put("defaultModel", provider.getDefaultModel());
        snapshot.put("customHeaders", maskSensitiveHeaders(parseHeaders(provider.getCustomHeadersJson())));
        snapshot.put("enabled", provider.getEnabled());
        snapshot.put("platformRateLimitPerMinute", provider.getPlatformRateLimitPerMinute());
        snapshot.put("taskRateLimitPerMinute", provider.getTaskRateLimitPerMinute());
        snapshot.put("userRateLimitPerMinute", provider.getUserRateLimitPerMinute());
        snapshot.put("supportVision", Boolean.TRUE.equals(provider.getSupportVision()));
        snapshot.put("supportMultiImage", Boolean.TRUE.equals(provider.getSupportMultiImage()));
        snapshot.put("maxImageCount", provider.getMaxImageCount() != null ? provider.getMaxImageCount() : 10);
        snapshot.put("visionModel", provider.getVisionModel());
        snapshot.put("structuredOutputMode",
                hasText(provider.getStructuredOutputMode()) ? provider.getStructuredOutputMode() : "NONE");
        snapshot.put("apiKeyConfigured", hasText(provider.getEncryptedApiKey()));
        return snapshot;
    }

    public ProviderCapability capability(LlmProvider provider) {
        if (provider == null) {
            return ProviderCapability.textOnly();
        }
        return new ProviderCapability(
                Boolean.TRUE.equals(provider.getSupportVision()),
                Boolean.TRUE.equals(provider.getSupportMultiImage()),
                provider.getMaxImageCount() != null ? provider.getMaxImageCount() : 10,
                provider.getVisionModel(),
                hasText(provider.getStructuredOutputMode()) ? provider.getStructuredOutputMode() : "NONE"
        );
    }

    private Map<String, String> normalizeHeaders(Map<String, String> customHeaders) {
        if (customHeaders == null || customHeaders.isEmpty()) {
            return new LinkedHashMap<>();
        }
        Map<String, String> normalized = new LinkedHashMap<>();
        customHeaders.forEach((key, value) -> {
            if (!hasText(key) || !hasText(value)) {
                return;
            }
            String normalizedKey = key.trim();
            if (normalizedKey.contains("\r") || normalizedKey.contains("\n")) {
                throw new BusinessException(PROVIDER_HEADER_INVALID, "LLM Provider 请求头名称不合法");
            }
            normalized.put(normalizedKey, value.trim());
        });
        return normalized;
    }

    private Map<String, String> parseHeaders(String headersJson) {
        if (!hasText(headersJson)) {
            return new LinkedHashMap<>();
        }
        try {
            return new LinkedHashMap<>(objectMapper.readValue(headersJson, STRING_MAP));
        } catch (JsonProcessingException ex) {
            throw new BusinessException(PROVIDER_JSON_INVALID, "LLM Provider 请求头配置不合法");
        }
    }

    private String toJson(Map<String, String> headers) {
        try {
            return objectMapper.writeValueAsString(headers == null ? Collections.emptyMap() : headers);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(PROVIDER_JSON_INVALID, "LLM Provider 请求头配置不合法");
        }
    }

    private Map<String, String> maskSensitiveHeaders(Map<String, String> headers) {
        Map<String, String> masked = new LinkedHashMap<>();
        headers.forEach((key, value) -> masked.put(key, isSensitiveHeader(key) ? MASKED : value));
        return masked;
    }

    private boolean isSensitiveHeader(String headerName) {
        String normalized = headerName.toLowerCase(Locale.ROOT);
        return normalized.equals("authorization")
                || normalized.equals("cookie")
                || normalized.equals("x-api-key")
                || normalized.equals("api-key")
                || normalized.contains("secret")
                || normalized.contains("token");
    }

    private String decryptStoredApiKey(String encryptedApiKey) {
        if (!hasText(encryptedApiKey)) {
            return null;
        }
        return encryptor.decrypt(encryptedApiKey);
    }

    private String trimTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
