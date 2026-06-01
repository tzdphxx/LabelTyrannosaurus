package com.labelhub.modules.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.infrastructure.llm.LlmMessage;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class DefaultMediaPromptContextBuilder implements MediaPromptContextBuilder {

    private static final Pattern MD_IMAGE = Pattern.compile("!\\[.*?]\\((https?://[^)]+)\\)");
    private static final Pattern HTML_IMAGE = Pattern.compile("<img[^>]+src=[\"'](https?://[^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
    private static final Pattern HTTP_URL = Pattern.compile("https?://[^\\s\"'<>),}]+");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public MediaPromptResult build(MediaPromptInput input) {
        Map<String, Object> item = parseMap(input.itemJson());
        String mediaType = text(item.getOrDefault("media_type", "text")).toLowerCase(Locale.ROOT);
        ProviderCapability capability = input.providerCapability() != null ? input.providerCapability() : ProviderCapability.textOnly();
        List<String> limitations = new ArrayList<>();
        List<String> imageUrls = new ArrayList<>();
        StringBuilder text = new StringBuilder();
        text.append(input.promptTemplate() == null ? "" : input.promptTemplate()).append("\n\n");
        text.append("itemSnapshot: ").append(toJson(item)).append("\n");
        if (input.answerJson() != null) {
            text.append("answerJson: ").append(input.answerJson()).append("\n");
        }

        PromptMode mode = PromptMode.TEXT_ONLY;
        boolean degraded = false;

        switch (mediaType) {
            case "image" -> {
                String url = text(item.get("media_url"));
                if (url.isBlank()) {
                    limitations.add("MEDIA_URL_MISSING");
                    degraded = true;
                } else if (!isHttpUrl(url)) {
                    limitations.add("MEDIA_URL_INVALID");
                    degraded = true;
                } else if (visionAvailable(input, capability, limitations)) {
                    imageUrls.add(url);
                    mode = PromptMode.IMAGE_SINGLE;
                } else {
                    degraded = true;
                    text.append("mediaUrl: ").append(url).append("\n");
                }
            }
            case "video" -> {
                String transcript = text(item.get("video_transcript"));
                if (!transcript.isBlank()) {
                    text.append("videoTranscript: ").append(transcript).append("\n");
                }
                text.append("ownerMediaDescription: ").append(text(item.get("owner_media_description"))).append("\n");
                imageUrls.addAll(validHttpUrls(stringList(item.get("key_frame_urls")), limitations));
                if (imageUrls.isEmpty()) {
                    limitations.add("KEY_FRAME_MISSING");
                    degraded = true;
                } else if (visionAvailable(input, capability, limitations)) {
                    mode = PromptMode.VIDEO_KEYFRAMES;
                } else {
                    degraded = true;
                    imageUrls.clear();
                }
                if (transcript.isBlank()) {
                    limitations.add("TRANSCRIPT_MISSING");
                }
            }
            case "markdown" -> {
                String markdown = text(item.get("content_markdown"));
                text.append("contentMarkdown: ").append(markdown).append("\n");
                imageUrls.addAll(validHttpUrls(extractMarkdownImages(markdown), limitations));
                if (!imageUrls.isEmpty() && visionAvailable(input, capability, limitations)) {
                    mode = PromptMode.MARKDOWN_WITH_IMAGES;
                } else if (!imageUrls.isEmpty()) {
                    degraded = true;
                    imageUrls.clear();
                }
            }
            default -> mode = PromptMode.TEXT_ONLY;
        }

        int limit = imageLimit(input, capability);
        if (imageUrls.size() > limit) {
            limitations.add("IMAGE_COUNT_EXCEEDED");
            imageUrls = new ArrayList<>(imageUrls.subList(0, Math.max(0, limit)));
        }
        if (!imageUrls.isEmpty() && imageUrls.size() > 1 && mode == PromptMode.IMAGE_SINGLE) {
            mode = PromptMode.IMAGE_MULTI;
        }

        String promptSnapshot = snapshot(text.toString(), mediaType, mode, degraded, limitations, imageUrls);
        List<LlmMessage> messages = imageUrls.isEmpty()
                ? List.of(new LlmMessage("user", text.toString()))
                : List.of(LlmMessage.userParts(contentParts(text.toString(), imageUrls, input.visionDetail())));
        return new MediaPromptResult(messages, mode, degraded, List.copyOf(limitations), promptSnapshot,
                mediaUnderstanding(mediaType, mode, degraded, limitations, imageUrls, item));
    }

    private boolean visionAvailable(MediaPromptInput input, ProviderCapability capability, List<String> limitations) {
        if (!input.multimodalEnabled() || !capability.supportVision()) {
            limitations.add("MULTIMODAL_NOT_SUPPORTED");
            return false;
        }
        return true;
    }

    private int imageLimit(MediaPromptInput input, ProviderCapability capability) {
        int configured = input.maxImagesPerRequest() > 0 ? input.maxImagesPerRequest() : 5;
        int provider = capability.maxImageCount() > 0 ? capability.maxImageCount() : configured;
        int limit = Math.max(0, Math.min(configured, provider));
        if (!capability.supportMultiImage()) {
            limit = Math.min(limit, 1);
        }
        return limit;
    }

    private List<LlmMessage.ContentPart> contentParts(String prompt, List<String> imageUrls, String detail) {
        List<LlmMessage.ContentPart> parts = new ArrayList<>();
        parts.add(new LlmMessage.TextPart(prompt));
        for (String imageUrl : imageUrls) {
            parts.add(new LlmMessage.ImageUrlPart(imageUrl, detail == null || detail.isBlank() ? "auto" : detail));
        }
        return parts;
    }

    private List<String> extractMarkdownImages(String markdown) {
        List<String> urls = new ArrayList<>();
        addMatches(urls, MD_IMAGE.matcher(markdown == null ? "" : markdown));
        addMatches(urls, HTML_IMAGE.matcher(markdown == null ? "" : markdown));
        return urls;
    }

    private void addMatches(List<String> urls, Matcher matcher) {
        while (matcher.find()) {
            urls.add(matcher.group(1));
        }
    }

    private String snapshot(String prompt, String mediaType, PromptMode promptMode, boolean degraded,
                            List<String> limitations, List<String> imageUrls) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("promptTemplate", safePrompt(prompt));
        snapshot.put("mediaType", mediaType);
        snapshot.put("promptMode", promptMode.name());
        snapshot.put("degraded", degraded);
        snapshot.put("limitations", limitations);
        snapshot.put("imageCount", imageUrls.size());
        snapshot.put("images", imageUrls.stream().map(this::safeUrlSummary).toList());
        return toJson(snapshot);
    }

    private Map<String, Object> mediaUnderstanding(String mediaType, PromptMode promptMode, boolean degraded,
                                                   List<String> limitations, List<String> imageUrls,
                                                   Map<String, Object> item) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("mode", promptMode.name());
        summary.put("mediaType", mediaType);
        summary.put("usedMedia", !imageUrls.isEmpty());
        summary.put("usedKeyFrames", promptMode == PromptMode.VIDEO_KEYFRAMES && !imageUrls.isEmpty());
        summary.put("usedTranscript", !text(item.get("video_transcript")).isBlank());
        summary.put("degraded", degraded);
        summary.put("limitations", List.copyOf(limitations));
        summary.put("imageCount", imageUrls.size());
        return summary;
    }

    private List<String> validHttpUrls(List<String> urls, List<String> limitations) {
        List<String> valid = new ArrayList<>();
        for (String url : urls) {
            if (isHttpUrl(url)) {
                valid.add(url);
            } else if (url != null && !url.isBlank()) {
                limitations.add("MEDIA_URL_INVALID");
            }
        }
        return valid;
    }

    private boolean isHttpUrl(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme))
                    && uri.getHost() != null;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private Map<String, Object> safeUrlSummary(String url) {
        Map<String, Object> summary = new LinkedHashMap<>();
        try {
            URI uri = URI.create(url);
            summary.put("scheme", uri.getScheme());
            summary.put("host", uri.getHost());
            summary.put("pathHash", sha256(uri.getPath() == null ? "" : uri.getPath()));
            return summary;
        } catch (IllegalArgumentException ex) {
            summary.put("invalid", true);
            return summary;
        }
    }

    private String safePrompt(String prompt) {
        String preview = prompt.length() > 1200 ? prompt.substring(0, 1200) : prompt;
        Matcher matcher = HTTP_URL.matcher(preview);
        StringBuffer sanitized = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(sanitized, Matcher.quoteReplacement(toJson(safeUrlSummary(matcher.group()))));
        }
        matcher.appendTail(sanitized);
        return sanitized.toString();
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 16);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMap(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            Object value = objectMapper.readValue(json, Object.class);
            return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    private List<String> stringList(Object value) {
        if (!(value instanceof Iterable<?> iterable)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (Object item : iterable) {
            String text = text(item);
            if (!text.isBlank()) {
                result.add(text);
            }
        }
        return result;
    }

    private String text(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
