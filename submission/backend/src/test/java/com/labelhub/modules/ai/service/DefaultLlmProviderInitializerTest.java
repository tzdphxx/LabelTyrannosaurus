package com.labelhub.modules.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.labelhub.modules.ai.config.DefaultLlmProviderProperties;
import com.labelhub.modules.ai.domain.LlmProvider;
import com.labelhub.modules.ai.mapper.LlmProviderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DefaultLlmProviderInitializerTest {

    private static final String SECRET = "local-test-encryption-secret";

    @Mock
    private LlmProviderMapper llmProviderMapper;

    private LlmApiKeyEncryptor encryptor;

    @BeforeEach
    void setUp() {
        encryptor = new LlmApiKeyEncryptor(SECRET);
    }

    @Test
    void skipsDatabaseWriteWhenAnyRequiredProviderConfigIsBlank() {
        DefaultLlmProviderInitializer initializer = new DefaultLlmProviderInitializer(
                properties("", "sk-test", "qwen-plus"), llmProviderMapper, encryptor);

        initializer.run(null);

        verify(llmProviderMapper, never()).selectOne(any(Wrapper.class));
        verify(llmProviderMapper, never()).insert(any(LlmProvider.class));
        verify(llmProviderMapper, never()).updateById(any(LlmProvider.class));
    }

    @Test
    void insertsDefaultProviderWithEncryptedApiKeyWhenConfigIsComplete() {
        when(llmProviderMapper.selectOne(any(Wrapper.class))).thenReturn(null);
        when(llmProviderMapper.insert(any(LlmProvider.class))).thenReturn(1);
        DefaultLlmProviderInitializer initializer = new DefaultLlmProviderInitializer(
                properties("https://dashscope.aliyuncs.com/compatible-mode/v1", "sk-test", "qwen-plus"),
                llmProviderMapper,
                encryptor);

        initializer.run(null);

        ArgumentCaptor<LlmProvider> providerCaptor = ArgumentCaptor.forClass(LlmProvider.class);
        verify(llmProviderMapper).insert(providerCaptor.capture());
        LlmProvider provider = providerCaptor.getValue();
        assertThat(provider.getProviderCode()).isEqualTo("dashscope-default");
        assertThat(provider.getProviderName()).isEqualTo("DashScope Default");
        assertThat(provider.getBaseUrl()).isEqualTo("https://dashscope.aliyuncs.com/compatible-mode/v1");
        assertThat(provider.getDefaultModel()).isEqualTo("qwen-plus");
        assertThat(provider.getEnabled()).isTrue();
        assertThat(provider.getCustomHeadersJson()).isEqualTo("{}");
        assertThat(provider.getStructuredOutputMode()).isEqualTo("JSON_OBJECT");
        assertThat(provider.getEncryptedApiKey()).isNotEqualTo("sk-test");
        assertThat(encryptor.decrypt(provider.getEncryptedApiKey())).isEqualTo("sk-test");
    }

    @Test
    void updatesExistingDefaultProviderWhenConfigChanges() {
        LlmProvider existing = existingProvider("https://old.example/v1", "old-key", "old-model");
        when(llmProviderMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
        when(llmProviderMapper.updateById(any(LlmProvider.class))).thenReturn(1);
        DefaultLlmProviderInitializer initializer = new DefaultLlmProviderInitializer(
                properties("https://dashscope.aliyuncs.com/compatible-mode/v1", "sk-new", "qwen-plus"),
                llmProviderMapper,
                encryptor);

        initializer.run(null);

        ArgumentCaptor<LlmProvider> providerCaptor = ArgumentCaptor.forClass(LlmProvider.class);
        verify(llmProviderMapper).updateById(providerCaptor.capture());
        LlmProvider provider = providerCaptor.getValue();
        assertThat(provider.getBaseUrl()).isEqualTo("https://dashscope.aliyuncs.com/compatible-mode/v1");
        assertThat(provider.getDefaultModel()).isEqualTo("qwen-plus");
        assertThat(provider.getEnabled()).isTrue();
        assertThat(encryptor.decrypt(provider.getEncryptedApiKey())).isEqualTo("sk-new");
    }

    @Test
    void doesNotUpdateExistingProviderWhenConfigIsUnchanged() {
        LlmProvider existing = existingProvider(
                "https://dashscope.aliyuncs.com/compatible-mode/v1", "sk-test", "qwen-plus");
        existing.setProviderName("DashScope Default");
        existing.setEnabled(true);
        existing.setCustomHeadersJson("{}");
        existing.setStructuredOutputMode("JSON_OBJECT");
        existing.setSupportVision(false);
        existing.setSupportMultiImage(false);
        existing.setMaxImageCount(10);
        when(llmProviderMapper.selectOne(any(Wrapper.class))).thenReturn(existing);
        DefaultLlmProviderInitializer initializer = new DefaultLlmProviderInitializer(
                properties("https://dashscope.aliyuncs.com/compatible-mode/v1", "sk-test", "qwen-plus"),
                llmProviderMapper,
                encryptor);

        initializer.run(null);

        verify(llmProviderMapper, never()).updateById(any(LlmProvider.class));
    }

    private DefaultLlmProviderProperties properties(String baseUrl, String apiKey, String defaultModel) {
        return new DefaultLlmProviderProperties(
                "dashscope-default",
                "DashScope Default",
                baseUrl,
                apiKey,
                defaultModel);
    }

    private LlmProvider existingProvider(String baseUrl, String apiKey, String defaultModel) {
        LlmProvider provider = new LlmProvider();
        provider.setId(10L);
        provider.setProviderCode("dashscope-default");
        provider.setProviderName("Old Name");
        provider.setBaseUrl(baseUrl);
        provider.setEncryptedApiKey(encryptor.encrypt(apiKey));
        provider.setDefaultModel(defaultModel);
        provider.setEnabled(false);
        provider.setCustomHeadersJson(null);
        provider.setStructuredOutputMode(null);
        return provider;
    }
}
