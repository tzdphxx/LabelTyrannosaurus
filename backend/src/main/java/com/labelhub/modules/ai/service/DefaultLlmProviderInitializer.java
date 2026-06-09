package com.labelhub.modules.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.labelhub.modules.ai.config.DefaultLlmProviderProperties;
import com.labelhub.modules.ai.domain.LlmProvider;
import com.labelhub.modules.ai.mapper.LlmProviderMapper;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DefaultLlmProviderInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DefaultLlmProviderInitializer.class);
    private static final String EMPTY_HEADERS_JSON = "{}";
    private static final String STRUCTURED_OUTPUT_MODE = "JSON_OBJECT";
    private static final int DEFAULT_MAX_IMAGE_COUNT = 10;

    private final DefaultLlmProviderProperties properties;
    private final LlmProviderMapper llmProviderMapper;
    private final LlmApiKeyEncryptor encryptor;

    public DefaultLlmProviderInitializer(DefaultLlmProviderProperties properties,
                                         LlmProviderMapper llmProviderMapper,
                                         LlmApiKeyEncryptor encryptor) {
        this.properties = properties;
        this.llmProviderMapper = llmProviderMapper;
        this.encryptor = encryptor;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.hasRequiredRuntimeConfig()) {
            log.info("Skipping default LLM provider initialization because baseUrl/apiKey/defaultModel is blank");
            return;
        }

        String providerCode = properties.normalizedProviderCode();
        LlmProvider existing = llmProviderMapper.selectOne(new QueryWrapper<LlmProvider>()
                .eq("provider_code", providerCode)
                .last("LIMIT 1"));
        if (existing == null) {
            llmProviderMapper.insert(newProvider(providerCode));
            return;
        }
        updateIfChanged(existing);
    }

    private LlmProvider newProvider(String providerCode) {
        LlmProvider provider = new LlmProvider();
        provider.setProviderCode(providerCode);
        applyConfiguredFields(provider);
        provider.setEncryptedApiKey(encryptor.encrypt(properties.normalizedApiKey()));
        return provider;
    }

    private void updateIfChanged(LlmProvider provider) {
        boolean changed = applyConfiguredFields(provider);
        String configuredApiKey = properties.normalizedApiKey();
        if (!apiKeyMatches(provider.getEncryptedApiKey(), configuredApiKey)) {
            provider.setEncryptedApiKey(encryptor.encrypt(configuredApiKey));
            changed = true;
        }
        if (changed) {
            llmProviderMapper.updateById(provider);
        }
    }

    private boolean applyConfiguredFields(LlmProvider provider) {
        boolean changed = false;
        changed |= setIfDifferent(provider::getProviderName, provider::setProviderName,
                properties.normalizedProviderName());
        changed |= setIfDifferent(provider::getBaseUrl, provider::setBaseUrl, properties.normalizedBaseUrl());
        changed |= setIfDifferent(provider::getDefaultModel, provider::setDefaultModel,
                properties.normalizedDefaultModel());
        changed |= setIfDifferent(provider::getCustomHeadersJson, provider::setCustomHeadersJson, EMPTY_HEADERS_JSON);
        changed |= setIfDifferent(provider::getEnabled, provider::setEnabled, true);
        changed |= setIfDifferent(provider::getSupportVision, provider::setSupportVision, false);
        changed |= setIfDifferent(provider::getSupportMultiImage, provider::setSupportMultiImage, false);
        changed |= setIfDifferent(provider::getMaxImageCount, provider::setMaxImageCount, DEFAULT_MAX_IMAGE_COUNT);
        changed |= setIfDifferent(provider::getStructuredOutputMode, provider::setStructuredOutputMode,
                STRUCTURED_OUTPUT_MODE);
        return changed;
    }

    private boolean apiKeyMatches(String encryptedApiKey, String configuredApiKey) {
        if (encryptedApiKey == null || encryptedApiKey.isBlank()) {
            return false;
        }
        try {
            return Objects.equals(encryptor.decrypt(encryptedApiKey), configuredApiKey);
        } catch (RuntimeException ex) {
            log.warn("Default LLM provider API key could not be decrypted; it will be replaced");
            return false;
        }
    }

    private <T> boolean setIfDifferent(java.util.function.Supplier<T> getter,
                                       java.util.function.Consumer<T> setter,
                                       T value) {
        if (Objects.equals(getter.get(), value)) {
            return false;
        }
        setter.accept(value);
        return true;
    }
}
