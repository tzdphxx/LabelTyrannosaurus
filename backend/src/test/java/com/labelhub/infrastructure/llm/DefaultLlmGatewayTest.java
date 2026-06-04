package com.labelhub.infrastructure.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.labelhub.modules.ai.service.LlmProviderRuntimeConfig;
import com.labelhub.modules.ai.service.LlmProviderService;
import com.labelhub.modules.ai.service.ProviderCapability;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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
        when(adapter.chat(any(), any(), any(), any(), any())).thenReturn(OpenAiCompatibleResponse.success(200,
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
        verify(adapter).chat(any(), messagesCaptor.capture(), any(), any(), any());
        assertThat(messagesCaptor.getValue()).extracting(LlmMessage::content).contains("answer");
    }

    @Test
    void recordsLlmGatewayMetricsForSuccessAndFailures() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        DefaultLlmGateway meteredGateway = new DefaultLlmGateway(llmProviderService, adapter,
                new com.fasterxml.jackson.databind.ObjectMapper(), new AiMetrics(registry));
        when(llmProviderService.findEnabledRuntimeConfig(PROVIDER_ID, "qwen-max"))
                .thenReturn(Optional.of(config("qwen-max")));
        when(adapter.chat(any(), any(), any(), any(), any()))
                .thenReturn(OpenAiCompatibleResponse.success(200,
                        "{\"choices\":[{\"message\":{\"content\":\"{\\\"decision\\\":\\\"PASS\\\"}\"}}]}",
                        18L))
                .thenReturn(OpenAiCompatibleResponse.failure(500,
                        "{\"error\":\"bad\"}", 9L, "Provider call failed", false));

        meteredGateway.review(request(PROVIDER_ID, "qwen-max", "answer"));
        meteredGateway.review(request(PROVIDER_ID, "qwen-max", "answer"));

        assertThat(registry.find("labelhub.ai.requests")
                .tag("biz_type", "LLM_GATEWAY")
                .tag("provider_id", "10")
                .tag("model_name", "qwen-max")
                .tag("status", "SUCCESS")
                .counter().count()).isEqualTo(1.0);
        assertThat(registry.find("labelhub.ai.requests")
                .tag("status", "PROVIDER_ERROR")
                .tag("error_code", "PROVIDER_ERROR")
                .counter().count()).isEqualTo(1.0);
        assertThat(registry.find("labelhub.ai.latency")
                .tag("biz_type", "LLM_GATEWAY")
                .tag("status", "SUCCESS")
                .timer().count()).isEqualTo(1L);
        assertThat(registry.find("labelhub.ai.latency")
                .tag("biz_type", "LLM_GATEWAY")
                .tag("status", "PROVIDER_ERROR")
                .tag("error_code", "PROVIDER_ERROR")
                .timer().count()).isEqualTo(1L);
    }

    @Test
    void disabledOrMissingProviderDoesNotCallAdapter() {
        when(llmProviderService.findEnabledRuntimeConfig(PROVIDER_ID, null)).thenReturn(Optional.empty());

        LlmGatewayResponse response = gateway.review(request(PROVIDER_ID, null, "answer"));

        assertThat(response.status()).isEqualTo(LlmGatewayStatus.PROVIDER_UNAVAILABLE);
        assertThat(response.errorCode()).isEqualTo("PROVIDER_UNAVAILABLE");
        verify(adapter, never()).chat(any(), any(), any(), any(), any());
    }

    @Test
    void mapsProviderErrorAndKeepsRawResponse() {
        when(llmProviderService.findEnabledRuntimeConfig(PROVIDER_ID, null))
                .thenReturn(Optional.of(config("qwen-plus")));
        when(adapter.chat(any(), any(), any(), any(), any())).thenReturn(OpenAiCompatibleResponse.failure(500,
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
        when(adapter.chat(any(), any(), any(), any(), any())).thenReturn(OpenAiCompatibleResponse.success(200,
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
        when(adapter.chat(any(), any(), any(), any(), any())).thenReturn(OpenAiCompatibleResponse.success(200,
                "{\"choices\":[{\"message\":{\"content\":\"{\\\"decision\\\":\\\"PASS\\\"}\"}}]}",
                5L));

        assertThat(gateway.review(request(10L, null, "a")).status()).isEqualTo(LlmGatewayStatus.SUCCESS);
        assertThat(gateway.review(request(20L, null, "b")).status()).isEqualTo(LlmGatewayStatus.SUCCESS);
        verify(adapter).chat(eq(config("qwen-plus")), eq(List.of(new LlmMessage("user", "a"))), any(), any(), any());
        verify(adapter).chat(eq(config("gpt-4o-mini")), eq(List.of(new LlmMessage("user", "b"))), any(), any(), any());
    }

    @Test
    void multimodalRequestUsesConfiguredVisionModel() {
        when(llmProviderService.findEnabledRuntimeConfig(PROVIDER_ID, "qwen-plus"))
                .thenReturn(Optional.of(new LlmProviderRuntimeConfig(
                        "https://example.test/v1",
                        "sk-test",
                        "qwen-plus",
                        Map.of(),
                        new ProviderCapability(true, true, 10, "qwen-vl-plus")
                )));
        when(adapter.chat(any(), any(), any(), any(), any())).thenReturn(OpenAiCompatibleResponse.success(200,
                "{\"choices\":[{\"message\":{\"content\":\"{\\\"decision\\\":\\\"PASS\\\"}\"}}]}",
                5L));

        gateway.review(new LlmGatewayRequest(PROVIDER_ID, "qwen-plus", List.of(
                LlmMessage.userParts(List.of(
                        new LlmMessage.TextPart("look"),
                        new LlmMessage.ImageUrlPart("https://e.com/a.jpg", "auto")
                ))
        )));

        ArgumentCaptor<LlmProviderRuntimeConfig> configCaptor =
                ArgumentCaptor.forClass(LlmProviderRuntimeConfig.class);
        verify(adapter).chat(configCaptor.capture(), any(), any(), any(), any());
        assertThat(configCaptor.getValue().modelName()).isEqualTo("qwen-vl-plus");
    }

    private LlmGatewayRequest request(Long providerId, String modelName, String content) {
        return new LlmGatewayRequest(providerId, modelName, List.of(new LlmMessage("user", content)));
    }

    private LlmProviderRuntimeConfig config(String modelName) {
        return new LlmProviderRuntimeConfig("https://example.test/v1", "sk-test", modelName, Map.of("X-Test", "yes"));
    }
}
