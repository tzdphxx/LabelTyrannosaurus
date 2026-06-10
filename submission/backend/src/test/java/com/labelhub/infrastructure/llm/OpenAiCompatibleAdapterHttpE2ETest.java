package com.labelhub.infrastructure.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.modules.ai.service.LlmProviderRuntimeConfig;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * E2E 验证用例（阶段 3）：真实 HTTP 链路的成功 / 失败 / 超时 / 脱敏。
 * 用 JDK 内置 HttpServer 起本地假模型；adapter 走包级构造器（validateUrls=false）以允许 localhost。
 */
class OpenAiCompatibleAdapterHttpE2ETest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    private String startServer(int status, String body, long delayMs) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            try {
                if (delayMs > 0) {
                    Thread.sleep(delayMs);
                }
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/v1";
    }

    private OpenAiCompatibleResponse call(String baseUrl, Duration timeout) {
        OpenAiCompatibleAdapter adapter = new OpenAiCompatibleAdapter(new ObjectMapper(), timeout);
        LlmProviderRuntimeConfig config = new LlmProviderRuntimeConfig(
                baseUrl, "sk-LIVE-SECRET-zzz", "qwen-plus", Map.of());
        return adapter.chat(config, List.of(new LlmMessage("user", "ping")));
    }

    @Test
    void successResponseReturnsBody() throws Exception {
        String base = startServer(200, "{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}", 0);
        OpenAiCompatibleResponse resp = call(base, Duration.ofSeconds(5));

        assertThat(resp.success()).isTrue();
        assertThat(resp.httpStatus()).isEqualTo(200);
        assertThat(resp.rawResponse()).contains("choices");
        assertThat(resp.timedOut()).isFalse();
    }

    @Test
    void non2xxReturnsFailureWithoutLeakingApiKey() throws Exception {
        String base = startServer(401, "{\"error\":\"bad key sk-LIVE-SECRET-zzz\"}", 0);
        OpenAiCompatibleResponse resp = call(base, Duration.ofSeconds(5));

        assertThat(resp.success()).isFalse();
        assertThat(resp.httpStatus()).isEqualTo(401);
        assertThat(resp.timedOut()).isFalse();
        // 错误信息中 apiKey 必须被脱敏
        assertThat(resp.errorMessage()).doesNotContain("sk-LIVE-SECRET-zzz");
        assertThat(resp.errorMessage()).contains("***");
    }

    @Test
    void slowResponseFailsAsRequestTimeout() throws Exception {
        // 读超时（请求级 timeout）：JDK 抛 HttpTimeoutException，落入 IOException 分支。
        // 实测行为：success=false，但 timedOut=false（timedOut 仅对 HttpConnectTimeoutException 为 true）。
        // 这是生产代码的真实分类特征，在此锁定。
        String base = startServer(200, "{}", 1500);
        OpenAiCompatibleResponse resp = call(base, Duration.ofMillis(300));

        assertThat(resp.success()).isFalse();
        assertThat(resp.timedOut()).isFalse();
        assertThat(resp.errorMessage()).contains("I/O");
    }

    @Test
    void connectionRefusedReturnsFailure() {
        // 指向一个未监听的本地端口
        OpenAiCompatibleResponse resp = call("http://127.0.0.1:1", Duration.ofSeconds(2));

        assertThat(resp.success()).isFalse();
        assertThat(resp.rawResponse()).isNull();
    }
}
