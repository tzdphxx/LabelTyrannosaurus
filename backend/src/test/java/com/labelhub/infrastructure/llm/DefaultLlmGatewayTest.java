package com.labelhub.infrastructure.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.modules.ai.service.LlmProviderRuntimeConfig;
import com.labelhub.modules.ai.service.LlmProviderService;
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
class DefaultLlmGatewayTest {

    private static final Long PROVIDER_ID = 10L;

    @Mock
    private LlmProviderService llmProviderService;

    @Mock
    private OpenAiCompatibleAdapter adapter;

    private DefaultLlmGateway gateway;

    @BeforeEach
    void setUp() {
        gateway = new DefaultLlmGateway(llmProviderService, adapter);
    }

    @Test
    void reviewsThroughEnabledProviderAndExtractsFencedJson() {
        when(llmProviderService.findEnabledRuntimeConfig(PROVIDER_ID, "qwen-max"))
                .thenReturn(Optional.of(config("qwen-max")));
        when(adapter.chat(any(), any())).thenReturn(OpenAiCompatibleResponse.success(200,
                "{\"choices\":[{\"message\":{\"content\":\"```json\\n{\\\"decision\\\":\\\"PASS\\\",\\\"score\\\":96}\\n```\"}}]}",
                18L));

        LlmGatewayResponse response = gateway.review(request(PROVIDER_ID, "qwen-max", "answer"));

        assertThat(response.status()).isEqualTo(LlmGatewayStatus.SUCCESS);
        assertThat(response.contentText()).contains("decision");
        assertThat(response.structuredJson()).containsEntry("decision", "PASS");
        assertThat(response.structuredJson()).containsEntry("score", 96);
        assertThat(response.rawResponse()).contains("\"choices\"");
        assertThat(response.latencyMs()).isEqualTo(18L);
        ArgumentCaptor<List<LlmMessage>> messagesCaptor = ArgumentCaptor.forClass(List.class);
        verify(adapter).chat(any(), messagesCaptor.capture());
        assertThat(messagesCaptor.getValue()).extracting(LlmMessage::content).contains("answer");
    }

    @Test
    void disabledOrMissingProviderDoesNotCallAdapter() {
        when(llmProviderService.findEnabledRuntimeConfig(PROVIDER_ID, null)).thenReturn(Optional.empty());

        LlmGatewayResponse response = gateway.review(request(PROVIDER_ID, null, "answer"));

        assertThat(response.status()).isEqualTo(LlmGatewayStatus.PROVIDER_UNAVAILABLE);
        assertThat(response.errorCode()).isEqualTo("PROVIDER_UNAVAILABLE");
        verify(adapter, never()).chat(any(), any());
    }

    @Test
    void mapsProviderErrorAndKeepsRawResponse() {
        when(llmProviderService.findEnabledRuntimeConfig(PROVIDER_ID, null))
                .thenReturn(Optional.of(config("qwen-plus")));
        when(adapter.chat(any(), any())).thenReturn(OpenAiCompatibleResponse.failure(500,
                "{\"error\":\"bad\"}", 9L, "Provider call failed with status 500", false));

        LlmGatewayResponse response = gateway.review(request(PROVIDER_ID, null, "answer"));

        assertThat(response.status()).isEqualTo(LlmGatewayStatus.PROVIDER_ERROR);
        assertThat(response.errorCode()).isEqualTo("PROVIDER_ERROR");
        assertThat(response.rawResponse()).contains("bad");
    }

    @Test
    void mapsNonJsonModelContentToInvalidJsonAndPreservesContent() {
        when(llmProviderService.findEnabledRuntimeConfig(PROVIDER_ID, null))
                .thenReturn(Optional.of(config("qwen-plus")));
        when(adapter.chat(any(), any())).thenReturn(OpenAiCompatibleResponse.success(200,
                "{\"choices\":[{\"message\":{\"content\":\"plain review text\"}}]}",
                11L));

        LlmGatewayResponse response = gateway.review(request(PROVIDER_ID, null, "answer"));

        assertThat(response.status()).isEqualTo(LlmGatewayStatus.INVALID_JSON);
        assertThat(response.errorCode()).isEqualTo("INVALID_JSON");
        assertThat(response.contentText()).isEqualTo("plain review text");
        assertThat(response.rawResponse()).contains("plain review text");
    }

    @Test
    void differentProvidersUseSameGatewayPath() {
        when(llmProviderService.findEnabledRuntimeConfig(10L, null)).thenReturn(Optional.of(config("qwen-plus")));
        when(llmProviderService.findEnabledRuntimeConfig(20L, null)).thenReturn(Optional.of(config("gpt-4o-mini")));
        when(adapter.chat(any(), any())).thenReturn(OpenAiCompatibleResponse.success(200,
                "{\"choices\":[{\"message\":{\"content\":\"{\\\"decision\\\":\\\"PASS\\\"}\"}}]}",
                5L));

        assertThat(gateway.review(request(10L, null, "a")).status()).isEqualTo(LlmGatewayStatus.SUCCESS);
        assertThat(gateway.review(request(20L, null, "b")).status()).isEqualTo(LlmGatewayStatus.SUCCESS);
        verify(adapter).chat(config("qwen-plus"), List.of(new LlmMessage("user", "a")));
        verify(adapter).chat(config("gpt-4o-mini"), List.of(new LlmMessage("user", "b")));
    }

    private LlmGatewayRequest request(Long providerId, String modelName, String content) {
        return new LlmGatewayRequest(providerId, modelName, List.of(new LlmMessage("user", content)));
    }

    private LlmProviderRuntimeConfig config(String modelName) {
        return new LlmProviderRuntimeConfig("https://example.test/v1", "sk-test", modelName, Map.of("X-Test", "yes"));
    }
}
