package com.labelhub.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.infrastructure.llm.LlmMessage;
import com.labelhub.infrastructure.llm.OpenAiCompatibleAdapter;
import com.labelhub.infrastructure.llm.OpenAiCompatibleResponse;
import com.labelhub.infrastructure.storage.CosObjectStorageService;
import com.labelhub.modules.ai.service.LlmProviderRuntimeConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ExternalConnectivityTest {

    private static final String TEST_IMAGE_URL = "https://www.w3schools.com/w3css/img_lights.jpg";
    private static final String TEST_VIDEO_URL = "http://vjs.zencdn.net/v/oceans.mp4";

    @Test
    void cosUploadReadPresignDownloadAndCleanupUsingDotenv() throws Exception {
        assumeTrue(Boolean.getBoolean("external.connectivity"),
                "set -Dexternal.connectivity=true to run external connectivity tests");
        Map<String, String> env = dotenv();
        String secretId = required(env, "COS_SECRET_ID");
        String secretKey = required(env, "COS_SECRET_KEY");
        String region = required(env, "COS_REGION");
        String bucket = required(env, "COS_BUCKET");
        String endpoint = env.getOrDefault("COS_ENDPOINT", "");
        assumeTrue(!isPlaceholder(secretId) && !isPlaceholder(secretKey) && !isPlaceholder(bucket),
                "COS env is missing or placeholder");

        COSCredentials credentials = new BasicCOSCredentials(secretId, secretKey);
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        if (!endpoint.isBlank()) {
            clientConfig.setEndPointSuffix(endpoint);
        }
        COSClient cosClient = new COSClient(credentials, clientConfig);
        CosObjectStorageService storage = new CosObjectStorageService(cosClient);
        String objectKey = "connectivity-test/labelhub-" + System.currentTimeMillis() + ".txt";
        String payload = "labelhub-cos-connectivity-" + Instant.now();
        try {
            byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
            storage.upload(bucket, objectKey, "text/plain",
                    new ByteArrayInputStream(bytes), bytes.length);

            try (InputStream inputStream = storage.openReadStream(bucket, objectKey)) {
                assertThat(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8))
                        .isEqualTo(payload);
            }

            URL signedUrl = storage.generatePresignedDownloadUrl(
                    bucket, objectKey, "connectivity.txt", Instant.now().plus(Duration.ofMinutes(5)));
            HttpResponse<String> response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create(signedUrl.toString())).GET().build(),
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            assertThat(response.statusCode()).isBetween(200, 299);
            assertThat(response.body()).isEqualTo(payload);
        } finally {
            cosClient.deleteObject(bucket, objectKey);
            cosClient.shutdown();
        }
    }

    @Test
    void dashscopeCompatibleChatUsingDotenvWhenApiKeyExists() {
        assumeTrue(Boolean.getBoolean("external.connectivity"),
                "set -Dexternal.connectivity=true to run external connectivity tests");
        Map<String, String> env = dotenv();
        String apiKey = aiApiKey(env);
        assumeTrue(!apiKey.isBlank() && !isPlaceholder(apiKey), "AI_DASHSCOPE_API_KEY is not configured in .env");

        String baseUrl = env.getOrDefault("AI_DASHSCOPE_BASE_URL",
                "https://dashscope.aliyuncs.com/compatible-mode/v1");
        String model = env.getOrDefault("AI_DASHSCOPE_CHAT_MODEL", "qwen-plus");
        OpenAiCompatibleAdapter adapter = new OpenAiCompatibleAdapter(
                new ObjectMapper(), 30_000L, 60_000L);

        OpenAiCompatibleResponse response = adapter.chat(
                new LlmProviderRuntimeConfig(baseUrl, apiKey, model, Map.of()),
                List.of(new LlmMessage("user", "ping")),
                8);

        assertThat(response.success()).as(response.errorMessage()).isTrue();
    }

    @Test
    void importedImageUploadsToCosAndAiReturnsAnswer() throws Exception {
        assumeTrue(Boolean.getBoolean("external.connectivity"),
                "set -Dexternal.connectivity=true to run external connectivity tests");
        Map<String, String> env = dotenv();
        String apiKey = aiApiKey(env);
        assumeTrue(!apiKey.isBlank() && !isPlaceholder(apiKey), "AI API key is not configured");

        CosFixture cos = cosFixture(env);
        byte[] image = downloadBytes(TEST_IMAGE_URL);
        String objectKey = "connectivity-test/import-image-" + System.currentTimeMillis() + ".jpg";
        try {
            cos.storage.upload(cos.bucket, objectKey, "image/jpeg", new ByteArrayInputStream(image), image.length);
            printKeptObjectKeyIfNeeded(objectKey);
            URL signedUrl = cos.storage.generatePresignedDownloadUrl(
                    cos.bucket, objectKey, "img_lights.jpg", Instant.now().plus(Duration.ofMinutes(10)));

            String answer = chatWithMediaUrl(env, apiKey, "image_url", signedUrl.toString(),
                    "请用一句中文描述这张图片。");
            printAiAnswerIfNeeded("IMAGE", answer);

            assertThat(answer).isNotBlank();
        } finally {
            deleteUnlessKeeping(cos.client, cos.bucket, objectKey);
            cos.client.shutdown();
        }
    }

    @Test
    void importedVideoUploadsToCosAndAiReturnsAnswer() throws Exception {
        assumeTrue(Boolean.getBoolean("external.connectivity"),
                "set -Dexternal.connectivity=true to run external connectivity tests");
        Map<String, String> env = dotenv();
        String apiKey = aiApiKey(env);
        assumeTrue(!apiKey.isBlank() && !isPlaceholder(apiKey), "AI API key is not configured");

        CosFixture cos = cosFixture(env);
        byte[] video = downloadBytes(TEST_VIDEO_URL);
        String objectKey = "connectivity-test/import-video-" + System.currentTimeMillis() + ".mp4";
        try {
            cos.storage.upload(cos.bucket, objectKey, "video/mp4", new ByteArrayInputStream(video), video.length);
            printKeptObjectKeyIfNeeded(objectKey);
            URL signedUrl = cos.storage.generatePresignedDownloadUrl(
                    cos.bucket, objectKey, "oceans.mp4", Instant.now().plus(Duration.ofMinutes(10)));

            String answer = chatWithMediaUrl(env, apiKey, "video_url", signedUrl.toString(),
                    "请用一句中文概括这个视频里主要出现了什么。");
            printAiAnswerIfNeeded("VIDEO", answer);

            assertThat(answer).isNotBlank();
        } finally {
            deleteUnlessKeeping(cos.client, cos.bucket, objectKey);
            cos.client.shutdown();
        }
    }

    private static Map<String, String> dotenv() {
        Path path = Path.of("..", ".env");
        assumeTrue(Files.exists(path), ".env not found");
        Map<String, String> values = new HashMap<>();
        try {
            for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (trimmed.isBlank() || trimmed.startsWith("#") || !trimmed.contains("=")) {
                    continue;
                }
                int index = trimmed.indexOf('=');
                values.put(trimmed.substring(0, index).trim(), trimmed.substring(index + 1).trim());
            }
            return values;
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to read .env", ex);
        }
    }

    private static String required(Map<String, String> env, String key) {
        String value = env.getOrDefault(key, "");
        assumeTrue(!value.isBlank(), key + " is not configured in .env");
        return value;
    }

    private static boolean isPlaceholder(String value) {
        return value == null
                || value.isBlank()
                || value.equals("replace-me")
                || value.contains("0000000000");
    }

    private static String aiApiKey(Map<String, String> env) {
        String value = System.getProperty("external.ai.api-key");
        if (value != null && !value.isBlank()) {
            return value;
        }
        return env.getOrDefault("AI_DASHSCOPE_API_KEY", "");
    }

    private static byte[] downloadBytes(String url) throws Exception {
        HttpResponse<byte[]> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create(url)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
        assertThat(response.statusCode()).isBetween(200, 299);
        assertThat(response.body()).isNotEmpty();
        return response.body();
    }

    private static String chatWithMediaUrl(Map<String, String> env, String apiKey, String mediaType,
                                           String mediaUrl, String prompt) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        String baseUrl = System.getProperty("external.ai.base-url",
                env.getOrDefault("AI_DASHSCOPE_BASE_URL", "https://dashscope.aliyuncs.com/compatible-mode/v1"));
        OpenAiCompatibleAdapter adapter = new OpenAiCompatibleAdapter(objectMapper, 30_000L, 60_000L);
        List<String> models = configuredModels(env);
        List<String> failures = new ArrayList<>();
        for (String model : models) {
            OpenAiCompatibleResponse response = adapter.chat(
                    new LlmProviderRuntimeConfig(baseUrl, apiKey, model, Map.of()),
                    List.of(LlmMessage.userParts(List.of(
                            new LlmMessage.TextPart(prompt),
                            mediaContentPart(mediaType, mediaUrl)
                    ))),
                    128);
            if (response.success()) {
                String content = objectMapper.readTree(response.rawResponse())
                        .path("choices").path(0).path("message").path("content").asText();
                if (!content.isBlank()) {
                    return content;
                }
                failures.add(model + ": empty response content");
            } else {
                failures.add(model + ": " + sanitizeError(response.errorMessage()));
            }
        }
        throw new AssertionError("No configured vision model returned an answer: " + failures);
    }

    private static List<String> configuredModels(Map<String, String> env) {
        String configured = System.getProperty("external.ai.vision-models",
                env.getOrDefault("AI_DASHSCOPE_VISION_MODELS", "qwen-vl-plus,qwen-vl-max-latest"));
        return List.of(configured.split(",")).stream()
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private static LlmMessage.ContentPart mediaContentPart(String mediaType, String mediaUrl) {
        if ("video_url".equals(mediaType)) {
            return new LlmMessage.VideoUrlPart(mediaUrl);
        }
        return new LlmMessage.ImageUrlPart(mediaUrl, "auto");
    }

    private static String sanitizeError(String body) {
        if (body == null) {
            return "";
        }
        return body.length() > 240 ? body.substring(0, 240) : body;
    }

    private static void deleteUnlessKeeping(COSClient client, String bucket, String objectKey) {
        if (!Boolean.getBoolean("external.keep-cos-object")) {
            client.deleteObject(bucket, objectKey);
        }
    }

    private static void printKeptObjectKeyIfNeeded(String objectKey) {
        if (Boolean.getBoolean("external.keep-cos-object")) {
            System.out.println("KEPT_COS_OBJECT_KEY=" + objectKey);
        }
    }

    private static void printAiAnswerIfNeeded(String label, String answer) {
        if (Boolean.getBoolean("external.print-ai-answer")) {
            System.out.println("AI_" + label + "_ANSWER=" + answer);
        }
    }

    private static CosFixture cosFixture(Map<String, String> env) {
        String secretId = required(env, "COS_SECRET_ID");
        String secretKey = required(env, "COS_SECRET_KEY");
        String region = required(env, "COS_REGION");
        String bucket = required(env, "COS_BUCKET");
        String endpoint = env.getOrDefault("COS_ENDPOINT", "");
        assumeTrue(!isPlaceholder(secretId) && !isPlaceholder(secretKey) && !isPlaceholder(bucket),
                "COS env is missing or placeholder");
        COSCredentials credentials = new BasicCOSCredentials(secretId, secretKey);
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        if (!endpoint.isBlank()) {
            clientConfig.setEndPointSuffix(endpoint);
        }
        COSClient client = new COSClient(credentials, clientConfig);
        return new CosFixture(client, new CosObjectStorageService(client), bucket);
    }

    private record CosFixture(COSClient client, CosObjectStorageService storage, String bucket) {
    }
}
