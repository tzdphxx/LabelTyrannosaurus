package com.labelhub.modules.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.labelhub.common.audit.AuditAppender;
import com.labelhub.common.audit.AuditCommand;
import com.labelhub.common.exception.BusinessException;
import com.labelhub.modules.ai.domain.LlmProvider;
import com.labelhub.modules.ai.dto.CreateLlmProviderRequest;
import com.labelhub.modules.ai.dto.LlmProviderResponse;
import com.labelhub.modules.ai.dto.LlmProviderTestResponse;
import com.labelhub.modules.ai.dto.TestLlmProviderRequest;
import com.labelhub.modules.ai.dto.UpdateLlmProviderRequest;
import com.labelhub.modules.ai.mapper.LlmProviderMapper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LlmProviderServiceTest {

    private static final Long ACTOR_ID = 1L;
    private static final Long PROVIDER_ID = 10L;
    private static final String SECRET = "local-test-encryption-secret";

    @Mock
    private LlmProviderMapper llmProviderMapper;

    @Mock
    private LlmProviderTester llmProviderTester;

    @Mock
    private AuditAppender auditAppender;

    private LlmApiKeyEncryptor encryptor;
    private LlmProviderService service;

    @BeforeEach
    void setUp() {
        encryptor = new LlmApiKeyEncryptor(SECRET);
        service = new LlmProviderService(llmProviderMapper, encryptor, llmProviderTester, auditAppender);
    }

    @Test
    void createsProviderWithEncryptedApiKeyAndMaskedResponse() {
        when(llmProviderMapper.insert(any(LlmProvider.class))).thenAnswer(invocation -> {
            LlmProvider provider = invocation.getArgument(0);
            provider.setId(PROVIDER_ID);
            return 1;
        });

        LlmProviderResponse response = service.create(ACTOR_ID, createRequest());

        assertThat(response.id()).isEqualTo(PROVIDER_ID);
        assertThat(response.apiKeyConfigured()).isTrue();
        assertThat(response.customHeaders()).containsEntry("Authorization", "******");
        assertThat(response.customHeaders()).containsEntry("X-Trace-Source", "labelhub");
        ArgumentCaptor<LlmProvider> providerCaptor = ArgumentCaptor.forClass(LlmProvider.class);
        verify(llmProviderMapper).insert(providerCaptor.capture());
        LlmProvider stored = providerCaptor.getValue();
        assertThat(stored.getEncryptedApiKey()).isNotEqualTo("sk-test");
        assertThat(encryptor.decrypt(stored.getEncryptedApiKey())).isEqualTo("sk-test");
        assertThat(stored.getCustomHeadersJson()).contains("Authorization").doesNotContain("unused");
        verify(auditAppender).append(any(AuditCommand.class));
    }

    @Test
    void updatesProviderAndKeepsExistingApiKeyWhenRequestOmitsIt() {
        LlmProvider provider = persistedProvider();
        String originalEncryptedKey = provider.getEncryptedApiKey();
        when(llmProviderMapper.selectById(PROVIDER_ID)).thenReturn(provider);
        when(llmProviderMapper.updateById(any(LlmProvider.class))).thenReturn(1);

        LlmProviderResponse response = service.update(ACTOR_ID, PROVIDER_ID, updateRequest(null));

        assertThat(response.defaultModel()).isEqualTo("qwen-plus");
        assertThat(provider.getEncryptedApiKey()).isEqualTo(originalEncryptedKey);
        verify(auditAppender).append(any(AuditCommand.class));
    }

    @Test
    void updatesProviderAndReplacesApiKeyWhenProvided() {
        LlmProvider provider = persistedProvider();
        when(llmProviderMapper.selectById(PROVIDER_ID)).thenReturn(provider);
        when(llmProviderMapper.updateById(any(LlmProvider.class))).thenReturn(1);

        service.update(ACTOR_ID, PROVIDER_ID, updateRequest("sk-new"));

        assertThat(encryptor.decrypt(provider.getEncryptedApiKey())).isEqualTo("sk-new");
    }

    @Test
    void disablesProviderAndFindEnabledProviderStopsReturningIt() {
        LlmProvider provider = persistedProvider();
        when(llmProviderMapper.selectById(PROVIDER_ID)).thenReturn(provider);
        when(llmProviderMapper.updateById(any(LlmProvider.class))).thenReturn(1);

        LlmProviderResponse disabled = service.disable(ACTOR_ID, PROVIDER_ID);

        assertThat(disabled.enabled()).isFalse();
        assertThat(service.findEnabledById(PROVIDER_ID)).isEmpty();
        verify(auditAppender).append(any(AuditCommand.class));
    }

    @Test
    void listsProvidersWithoutLeakingApiKeys() {
        LlmProvider provider = persistedProvider();
        when(llmProviderMapper.selectList(any(Wrapper.class))).thenReturn(List.of(provider));

        List<LlmProviderResponse> providers = service.list();

        assertThat(providers).hasSize(1);
        assertThat(providers.get(0).apiKeyConfigured()).isTrue();
        assertThat(providers.get(0).customHeaders()).containsEntry("Authorization", "******");
    }

    @Test
    void testsProviderWithStoredApiKeyAndRequestOverrides() {
        LlmProvider provider = persistedProvider();
        when(llmProviderMapper.selectById(PROVIDER_ID)).thenReturn(provider);
        when(llmProviderTester.test(any(LlmProviderRuntimeConfig.class)))
                .thenReturn(new LlmProviderTestResponse(true, 12L, "OK"));

        LlmProviderTestResponse response = service.test(PROVIDER_ID,
                new TestLlmProviderRequest(null, "qwen-max", Map.of("X-Test", "yes")));

        assertThat(response.success()).isTrue();
        ArgumentCaptor<LlmProviderRuntimeConfig> configCaptor = ArgumentCaptor.forClass(LlmProviderRuntimeConfig.class);
        verify(llmProviderTester).test(configCaptor.capture());
        assertThat(configCaptor.getValue().apiKey()).isEqualTo("sk-old");
        assertThat(configCaptor.getValue().modelName()).isEqualTo("qwen-max");
        assertThat(configCaptor.getValue().customHeaders()).containsEntry("X-Test", "yes");
    }

    @Test
    void throwsWhenProviderDoesNotExist() {
        when(llmProviderMapper.selectById(PROVIDER_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.disable(ACTOR_ID, PROVIDER_ID))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.getCode()).isEqualTo(404301));
    }

    private CreateLlmProviderRequest createRequest() {
        return new CreateLlmProviderRequest(
                "dashscope",
                "DashScope",
                "https://dashscope.aliyuncs.com/compatible-mode/v1",
                "sk-test",
                "qwen-plus",
                Map.of("Authorization", "Bearer custom", "X-Trace-Source", "labelhub", "unused", " "),
                60,
                30,
                10
        );
    }

    private UpdateLlmProviderRequest updateRequest(String apiKey) {
        return new UpdateLlmProviderRequest(
                "dashscope",
                "DashScope Updated",
                "https://dashscope.aliyuncs.com/compatible-mode/v1",
                apiKey,
                "qwen-plus",
                Map.of("Authorization", "Bearer updated", "X-Trace-Source", "labelhub"),
                120,
                40,
                20
        );
    }

    private LlmProvider persistedProvider() {
        LlmProvider provider = new LlmProvider();
        provider.setId(PROVIDER_ID);
        provider.setProviderCode("dashscope");
        provider.setProviderName("DashScope");
        provider.setBaseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1");
        provider.setEncryptedApiKey(encryptor.encrypt("sk-old"));
        provider.setDefaultModel("qwen-turbo");
        provider.setCustomHeadersJson("{\"Authorization\":\"Bearer custom\",\"X-Trace-Source\":\"labelhub\"}");
        provider.setEnabled(true);
        provider.setPlatformRateLimitPerMinute(60);
        provider.setTaskRateLimitPerMinute(30);
        provider.setUserRateLimitPerMinute(10);
        provider.setCreatedBy(ACTOR_ID);
        return provider;
    }
}
