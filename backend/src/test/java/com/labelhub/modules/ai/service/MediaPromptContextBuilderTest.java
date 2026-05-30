package com.labelhub.modules.ai.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.labelhub.infrastructure.llm.LlmMessage;
import java.util.List;
import org.junit.jupiter.api.Test;

class MediaPromptContextBuilderTest {

    private final DefaultMediaPromptContextBuilder builder = new DefaultMediaPromptContextBuilder();

    @Test
    void imageWithVisionProviderBuildsImageContentPart() {
        MediaPromptResult result = builder.build(new MediaPromptInput(
                "{\"media_type\":\"image\",\"media_url\":\"https://example.com/cat.jpg\"}",
                "{\"answer\":\"\"}",
                "Describe the item",
                new ProviderCapability(true, true, 5, null),
                true,
                "auto",
                5
        ));

        assertThat(result.promptMode()).isEqualTo(PromptMode.IMAGE_SINGLE);
        assertThat(result.degraded()).isFalse();
        assertThat(result.messages()).hasSize(1);
        assertThat(result.messages().get(0).contentParts())
                .anySatisfy(part -> assertThat(part).isInstanceOf(LlmMessage.ImageUrlPart.class));
    }

    @Test
    void imageWithoutVisionProviderDegradesToTextOnly() {
        MediaPromptResult result = builder.build(new MediaPromptInput(
                "{\"media_type\":\"image\",\"media_url\":\"https://example.com/cat.jpg\"}",
                "{}",
                "Describe the item",
                new ProviderCapability(false, false, 0, null),
                true,
                "auto",
                5
        ));

        assertThat(result.promptMode()).isEqualTo(PromptMode.TEXT_ONLY);
        assertThat(result.degraded()).isTrue();
        assertThat(result.limitations()).contains("MULTIMODAL_NOT_SUPPORTED");
        assertThat(result.messages().get(0).content()).contains("https://example.com/cat.jpg");
    }

    @Test
    void videoWithKeyframesCapsImagesAndRecordsLimitation() {
        MediaPromptResult result = builder.build(new MediaPromptInput(
                "{\"media_type\":\"video\",\"key_frame_urls\":[\"https://e.com/1.jpg\",\"https://e.com/2.jpg\"],\"video_transcript\":\"hello\"}",
                "{}",
                "Review video",
                new ProviderCapability(true, true, 1, null),
                true,
                "auto",
                1
        ));

        List<LlmMessage.ContentPart> parts = result.messages().get(0).contentParts();
        assertThat(result.promptMode()).isEqualTo(PromptMode.VIDEO_KEYFRAMES);
        assertThat(parts.stream().filter(LlmMessage.ImageUrlPart.class::isInstance)).hasSize(1);
        assertThat(result.limitations()).contains("IMAGE_COUNT_EXCEEDED");
    }

    @Test
    void markdownExtractsEmbeddedImages() {
        MediaPromptResult result = builder.build(new MediaPromptInput(
                "{\"media_type\":\"markdown\",\"content_markdown\":\"hello ![x](https://e.com/a.png) <img src=\\\"https://e.com/b.jpg\\\">\"}",
                "{}",
                "Review markdown",
                new ProviderCapability(true, true, 5, null),
                true,
                "auto",
                5
        ));

        assertThat(result.promptMode()).isEqualTo(PromptMode.MARKDOWN_WITH_IMAGES);
        assertThat(result.messages().get(0).contentParts().stream()
                .filter(LlmMessage.ImageUrlPart.class::isInstance)).hasSize(2);
    }

    @Test
    void missingImageUrlDegradesWithLimitation() {
        MediaPromptResult result = builder.build(new MediaPromptInput(
                "{\"media_type\":\"image\"}",
                "{}",
                "Describe image",
                new ProviderCapability(true, true, 5, null),
                true,
                "auto",
                5
        ));

        assertThat(result.degraded()).isTrue();
        assertThat(result.limitations()).contains("MEDIA_URL_MISSING");
    }
}
