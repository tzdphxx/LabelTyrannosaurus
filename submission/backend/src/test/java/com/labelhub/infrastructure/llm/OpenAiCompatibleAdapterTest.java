package com.labelhub.infrastructure.llm;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.modules.ai.service.LlmProviderRuntimeConfig;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class OpenAiCompatibleAdapterTest {

    @Test
    void serializesLegacyTextMessageAsStringContent() {
        OpenAiCompatibleAdapter adapter = new OpenAiCompatibleAdapter(new ObjectMapper(), Duration.ofSeconds(1));

        List<Map<String, Object>> serialized = ReflectionTestUtils.invokeMethod(
                adapter, "serializeMessages", List.of(new LlmMessage("user", "hello")));

        assertThat(serialized).hasSize(1);
        assertThat(serialized.get(0)).containsEntry("role", "user");
        assertThat(serialized.get(0)).containsEntry("content", "hello");
    }

    @Test
    void serializesMultimodalContentPartsAsOpenAiCompatibleBlocks() {
        OpenAiCompatibleAdapter adapter = new OpenAiCompatibleAdapter(new ObjectMapper(), Duration.ofSeconds(1));
        LlmMessage message = LlmMessage.userParts(List.of(
                new LlmMessage.TextPart("look"),
                new LlmMessage.ImageUrlPart("https://example.com/a.jpg", "auto")
        ));

        List<Map<String, Object>> serialized = ReflectionTestUtils.invokeMethod(
                adapter, "serializeMessages", List.of(message));

        assertThat(serialized).hasSize(1);
        assertThat(serialized.get(0).get("content")).isInstanceOf(List.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) serialized.get(0).get("content");
        assertThat(content.get(0)).containsEntry("type", "text").containsEntry("text", "look");
        assertThat(content.get(1)).containsEntry("type", "image_url");
        assertThat(content.get(1).get("image_url")).isEqualTo(Map.of(
                "url", "https://example.com/a.jpg",
                "detail", "auto"
        ));
    }

    @Test
    void serializesVideoContentPartAsOpenAiCompatibleBlock() {
        OpenAiCompatibleAdapter adapter = new OpenAiCompatibleAdapter(new ObjectMapper(), Duration.ofSeconds(1));
        LlmMessage message = LlmMessage.userParts(List.of(
                new LlmMessage.TextPart("watch"),
                new LlmMessage.VideoUrlPart("https://example.com/oceans.mp4")
        ));

        List<Map<String, Object>> serialized = ReflectionTestUtils.invokeMethod(
                adapter, "serializeMessages", List.of(message));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> content = (List<Map<String, Object>>) serialized.get(0).get("content");
        assertThat(content.get(1)).containsEntry("type", "video_url");
        assertThat(content.get(1).get("video_url")).isEqualTo(Map.of(
                "url", "https://example.com/oceans.mp4"
        ));
    }

    @Test
    void buildsValidatedRequestWithoutRestrictedHostHeader() {
        OpenAiCompatibleAdapter adapter = new OpenAiCompatibleAdapter(
                new ObjectMapper(), Duration.ofSeconds(1), Duration.ofSeconds(1), true);
        LlmProviderRuntimeConfig config = new LlmProviderRuntimeConfig(
                "http://93.184.216.34/v1", "sk-live", "qwen-plus", Map.of());

        HttpRequest request = ReflectionTestUtils.invokeMethod(
                adapter,
                "buildRequest",
                config,
                List.of(new LlmMessage("user", "ping")),
                null,
                null,
                ResponseFormat.none());

        assertThat(request.headers().firstValue("Host")).isEmpty();
    }
}
