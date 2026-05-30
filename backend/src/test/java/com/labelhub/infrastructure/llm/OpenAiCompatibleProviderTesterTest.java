package com.labelhub.infrastructure.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.modules.ai.dto.LlmProviderTestResponse;
import com.labelhub.modules.ai.service.LlmProviderRuntimeConfig;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class OpenAiCompatibleProviderTesterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void delegatesToReusableAdapter() {
        OpenAiCompatibleAdapter adapter = org.mockito.Mockito.mock(OpenAiCompatibleAdapter.class);
        org.mockito.Mockito.when(adapter.chat(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(1)))
                .thenReturn(OpenAiCompatibleResponse.success(200, "{\"choices\":[]}", 7L));
        OpenAiCompatibleProviderTester tester = new OpenAiCompatibleProviderTester(adapter);

        LlmProviderTestResponse response = tester.test(config("https://example.test/v1", "sk-live", "qwen-plus",
                Map.of("X-Test", "yes")));

        assertThat(response.success()).isTrue();
        assertThat(response.latencyMs()).isEqualTo(7L);
        org.mockito.Mockito.verify(adapter).chat(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(1));
    }

    @Test
    void sendsOpenAiCompatibleChatCompletionRequest() throws IOException {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> customHeader = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = startServer(exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            customHeader.set(exchange.getRequestHeaders().getFirst("X-Test"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 200, "{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}");
        });
        OpenAiCompatibleProviderTester tester = new OpenAiCompatibleProviderTester(objectMapper, Duration.ofSeconds(2));

        LlmProviderTestResponse response = tester.test(config(baseUrl(), "sk-live", "qwen-plus",
                Map.of("X-Test", "yes")));

        assertThat(response.success()).isTrue();
        assertThat(response.latencyMs()).isGreaterThanOrEqualTo(0L);
        assertThat(response.message()).isEqualTo("OK");
        assertThat(authorization.get()).isEqualTo("Bearer sk-live");
        assertThat(customHeader.get()).isEqualTo("yes");
        assertThat(requestBody.get()).contains("\"model\":\"qwen-plus\"");
        assertThat(requestBody.get()).contains("\"max_tokens\":1");
    }

    @Test
    void mapsNonSuccessStatusWithoutLeakingApiKey() throws IOException {
        server = startServer(exchange -> respond(exchange, 500, "{\"error\":\"sk-live failed\"}"));
        OpenAiCompatibleProviderTester tester = new OpenAiCompatibleProviderTester(objectMapper, Duration.ofSeconds(2));

        LlmProviderTestResponse response = tester.test(config(baseUrl(), "sk-live", "qwen-plus", Map.of()));

        assertThat(response.success()).isFalse();
        assertThat(response.message()).contains("status 500").doesNotContain("sk-live");
    }

    @Test
    void mapsIoFailureWithoutLeakingApiKey() {
        OpenAiCompatibleProviderTester tester = new OpenAiCompatibleProviderTester(objectMapper, Duration.ofMillis(200));

        LlmProviderTestResponse response = tester.test(config("http://127.0.0.1:1", "sk-live", "qwen-plus", Map.of()));

        assertThat(response.success()).isFalse();
        assertThat(response.message()).doesNotContain("sk-live");
    }

    private HttpServer startServer(ExchangeHandler handler) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext("/chat/completions", exchange -> {
            try {
                handler.handle(exchange);
            } finally {
                exchange.close();
            }
        });
        httpServer.start();
        return httpServer;
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] responseBytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(status, responseBytes.length);
        exchange.getResponseBody().write(responseBytes);
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private LlmProviderRuntimeConfig config(String baseUrl, String apiKey, String modelName, Map<String, String> headers) {
        return new LlmProviderRuntimeConfig(baseUrl, apiKey, modelName, headers);
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
