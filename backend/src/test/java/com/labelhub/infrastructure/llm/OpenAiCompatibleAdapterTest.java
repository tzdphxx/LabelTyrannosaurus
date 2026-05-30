package com.labelhub.infrastructure.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.modules.ai.service.LlmProviderRuntimeConfig;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class OpenAiCompatibleAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void sendsChatCompletionsRequestWithHeadersAndAuthorization() throws IOException {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> customHeader = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        server = startServer(exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            customHeader.set(exchange.getRequestHeaders().getFirst("X-Provider"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 200, "{\"choices\":[{\"message\":{\"content\":\"{\\\"decision\\\":\\\"PASS\\\"}\"}}]}");
        });
        OpenAiCompatibleAdapter adapter = new OpenAiCompatibleAdapter(objectMapper, Duration.ofSeconds(2));

        OpenAiCompatibleResponse response = adapter.chat(
                config("sk-live", "qwen-plus", Map.of("X-Provider", "dashscope")),
                List.of(new LlmMessage("system", "review"), new LlmMessage("user", "answer"))
        );

        assertThat(response.success()).isTrue();
        assertThat(response.httpStatus()).isEqualTo(200);
        assertThat(response.rawResponse()).contains("\"choices\"");
        assertThat(authorization.get()).isEqualTo("Bearer sk-live");
        assertThat(customHeader.get()).isEqualTo("dashscope");
        assertThat(body.get()).contains("\"model\":\"qwen-plus\"");
        assertThat(body.get()).contains("\"role\":\"system\"");
        assertThat(body.get()).contains("\"content\":\"answer\"");
        assertThat(body.get()).doesNotContain("\"max_tokens\"");
    }

    @Test
    void mapsNonSuccessStatusWithoutLeakingApiKey() throws IOException {
        server = startServer(exchange -> respond(exchange, 500, "{\"error\":\"sk-live failed\"}"));
        OpenAiCompatibleAdapter adapter = new OpenAiCompatibleAdapter(objectMapper, Duration.ofSeconds(2));

        OpenAiCompatibleResponse response = adapter.chat(config("sk-live", "qwen-plus", Map.of()),
                List.of(new LlmMessage("user", "ping")));

        assertThat(response.success()).isFalse();
        assertThat(response.httpStatus()).isEqualTo(500);
        assertThat(response.errorMessage()).contains("status 500").doesNotContain("sk-live");
        assertThat(response.rawResponse()).contains("sk-live failed");
    }

    @Test
    void mapsIoFailureWithoutLeakingApiKey() {
        OpenAiCompatibleAdapter adapter = new OpenAiCompatibleAdapter(objectMapper, Duration.ofMillis(200));

        OpenAiCompatibleResponse response = adapter.chat(
                new LlmProviderRuntimeConfig("http://127.0.0.1:1", "sk-live", "qwen-plus", Map.of()),
                List.of(new LlmMessage("user", "ping"))
        );

        assertThat(response.success()).isFalse();
        assertThat(response.timedOut()).isFalse();
        assertThat(response.errorMessage()).doesNotContain("sk-live");
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

    private LlmProviderRuntimeConfig config(String apiKey, String modelName, Map<String, String> headers) {
        return new LlmProviderRuntimeConfig("http://127.0.0.1:" + server.getAddress().getPort(), apiKey, modelName, headers);
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
