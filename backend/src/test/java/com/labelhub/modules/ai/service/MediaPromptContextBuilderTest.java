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
    void videoWithMediaUrlBuildsVideoContentPart() {
        MediaPromptResult result = builder.build(new MediaPromptInput(
                "{\"media_type\":\"video\",\"media_url\":\"https://cos.example.com/oceans.mp4\",\"video_transcript\":\"waves\"}",
                "{}",
                "Review video",
                new ProviderCapability(true, true, 5, null),
                true,
                "auto",
                5
        ));

        assertThat(result.promptMode()).isEqualTo(PromptMode.VIDEO_DIRECT);
        assertThat(result.degraded()).isFalse();
        assertThat(result.messages().get(0).contentParts())
                .anySatisfy(part -> assertThat(part).isInstanceOf(LlmMessage.VideoUrlPart.class));
        assertThat(result.mediaUnderstanding()).containsEntry("usedVideo", true);
    }

    @Test
    void videoWithMediaUrlWithoutVisionProviderDegradesToTextOnly() {
        MediaPromptResult result = builder.build(new MediaPromptInput(
                "{\"media_type\":\"video\",\"media_url\":\"https://cos.example.com/oceans.mp4\"}",
                "{}",
                "Review video",
                new ProviderCapability(false, false, 0, null),
                true,
                "auto",
                5
        ));

        assertThat(result.promptMode()).isEqualTo(PromptMode.TEXT_ONLY);
        assertThat(result.degraded()).isTrue();
        assertThat(result.limitations()).contains("MULTIMODAL_NOT_SUPPORTED");
        assertThat(result.messages().get(0).content()).contains("https://cos.example.com/oceans.mp4");
    }

    @Test
    void providerWithoutMultiImageSupportUsesOneImageOnly() {
        MediaPromptResult result = builder.build(new MediaPromptInput(
                "{\"media_type\":\"video\",\"key_frame_urls\":[\"https://e.com/1.jpg\",\"https://e.com/2.jpg\"],\"video_transcript\":\"hello\"}",
                "{}",
                "Review video",
                new ProviderCapability(true, false, 10, null),
                true,
                "auto",
                5
        ));

        assertThat(result.messages().get(0).contentParts().stream()
                .filter(LlmMessage.ImageUrlPart.class::isInstance)).hasSize(1);
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

    @Test
    void nonHttpImageUrlIsRejectedAndDoesNotCreateImagePart() {
        MediaPromptResult result = builder.build(new MediaPromptInput(
                "{\"media_type\":\"image\",\"media_url\":\"file:///etc/passwd\"}",
                "{}",
                "Describe image",
                new ProviderCapability(true, true, 5, null),
                true,
                "auto",
                5
        ));

        assertThat(result.degraded()).isTrue();
        assertThat(result.promptMode()).isEqualTo(PromptMode.TEXT_ONLY);
        assertThat(result.limitations()).contains("MEDIA_URL_INVALID");
        assertThat(result.messages().get(0).contentParts()).isNull();
    }

    @Test
    void promptSnapshotContainsSafeMediaSummaryWithoutSignedUrl() {
        MediaPromptResult result = builder.build(new MediaPromptInput(
                "{\"media_type\":\"image\",\"media_url\":\"https://cdn.example.com/image.jpg?signature=secret-token\"}",
                "{}",
                "Describe image",
                new ProviderCapability(true, true, 5, null),
                true,
                "auto",
                5
        ));

        assertThat(result.promptSnapshot()).contains("\"mediaType\":\"image\"");
        assertThat(result.promptSnapshot()).contains("\"imageCount\":1");
        assertThat(result.promptSnapshot()).contains("cdn.example.com");
        assertThat(result.promptSnapshot()).doesNotContain("secret-token");
        assertThat(result.mediaUnderstanding()).containsEntry("usedMedia", true);
        assertThat(result.mediaUnderstanding()).containsEntry("mode", "IMAGE_SINGLE");
    }

    @Test
    void mediaUnderstandingIncludesProcessingStatusAndContextLimitations() {
        MediaPromptResult result = builder.build(new MediaPromptInput(
                "{\"media_type\":\"video\",\"media_processing_status\":\"PARTIAL\",\"media_context_limitations\":[\"TRANSCRIPT_UNAVAILABLE\"],\"key_frame_urls\":[\"https://e.com/1.jpg\"]}",
                "{}",
                "Review video",
                new ProviderCapability(true, true, 5, null),
                true,
                "auto",
                5
        ));

        assertThat(result.degraded()).isTrue();
        assertThat(result.limitations()).contains("TRANSCRIPT_UNAVAILABLE", "TRANSCRIPT_MISSING");
        assertThat(result.mediaUnderstanding()).containsEntry("processingStatus", "PARTIAL");
    }
}
