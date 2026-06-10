package com.labelhub.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.labelhub.infrastructure.llmtask.LlmTaskWorker;
import com.labelhub.integration.support.IntegrationTestBase;
import com.labelhub.integration.support.TestFixtures;
import com.labelhub.modules.ai.service.LlmApiKeyEncryptor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@EnabledIfSystemProperty(named = "labelhub.external-it.enabled", matches = "true")
class ExternalLlmTriggerFlowIntegrationTest extends IntegrationTestBase {

    @Autowired
    private LlmApiKeyEncryptor apiKeyEncryptor;

    @Autowired
    private LlmTaskWorker llmTaskWorker;

    @DynamicPropertySource
    static void dashscopeProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.dashscope.api-key", ExternalLlmTriggerFlowIntegrationTest::springAiApiKey);
        registry.add("spring.ai.dashscope.agent.api-key", ExternalLlmTriggerFlowIntegrationTest::springAiApiKey);
    }

    @Test
    void ownerCreatesTaskAndLabelerTriggersLlmSuggestionAgainstDashscope() {
        String apiKey = aiApiKey();
        assumeTrue(!apiKey.isBlank(), "set -Dexternal.ai.api-key or AI_DASHSCOPE_API_KEY");

        TestFixtures.RegisteredUser owner = fixtures.register("trigger-owner", "OWNER");
        TestFixtures.RegisteredUser labeler = fixtures.register("trigger-labeler", "LABELER");
        TestFixtures.CreatedTemplate template = fixtures.createTemplate(owner.accessToken());
        Long providerId = createDashscopeProvider(apiKey);
        Long taskId = createTask(owner.accessToken(), template.templateVersionId(), providerId);
        appendItem(owner.accessToken(), taskId);
        expectOk(apiClient.post("/api/v1/tasks/" + taskId + "/publish", Map.of(), owner.accessToken()));

        Long assignmentId = claimOne(labeler.accessToken(), taskId);
        Map<String, Object> triggerRun = data(apiClient.post(
                "/api/v1/assignments/" + assignmentId + "/llm-triggers",
                Map.of(
                        "componentId", template.templateId(),
                        "currentAnswerJson", Map.of("answer", "draft"),
                        "userInstruction", "Fill the answer field with one concise English sentence."
                ),
                labeler.accessToken()));
        Long triggerRunId = number(triggerRun.get("triggerRunId"));
        assertThat(triggerRunId).isNotNull();
        assertThat(String.valueOf(triggerRun.get("status"))).isEqualTo("RUNNING");

        Map<String, Object> completedRun = awaitCompletedRun(triggerRunId, labeler.accessToken());
        assertThat(String.valueOf(completedRun.get("status")))
                .as("LLM trigger run response: %s", completedRun)
                .isEqualTo("SUCCESS");
        assertThat(list(completedRun.get("targetFields"))).contains("answer");
        assertThat(map(completedRun.get("patch")).get("answer"))
                .as("LLM trigger patch: %s", completedRun.get("patch"))
                .isInstanceOf(String.class);
        assertThat(String.valueOf(map(completedRun.get("patch")).get("answer"))).isNotBlank();
    }

    private Long createDashscopeProvider(String apiKey) {
        String providerCode = "dashscope-trigger-" + shortId();
        jdbcTemplate.update("""
                INSERT INTO llm_providers (
                    provider_code, provider_name, base_url, encrypted_api_key, default_model,
                    custom_headers_json, enabled, platform_rate_limit_per_minute,
                    task_rate_limit_per_minute, user_rate_limit_per_minute, support_vision,
                    support_multi_image, max_image_count, vision_model, structured_output_mode
                )
                VALUES (?, ?, ?, ?, ?, CAST(? AS JSON), 1, 120, 60, 30, 0, 0, 10, NULL, 'JSON_OBJECT')
                """,
                providerCode,
                "DashScope Trigger Integration",
                aiBaseUrl(),
                apiKeyEncryptor.encrypt(apiKey),
                aiModel(),
                "{}");
        return jdbcTemplate.queryForObject(
                "SELECT id FROM llm_providers WHERE provider_code = ?",
                Long.class,
                providerCode);
    }

    private Long createTask(String ownerToken, Long templateVersionId, Long providerId) {
        Map<String, Object> request = new java.util.LinkedHashMap<>();
        request.put("title", "llm-trigger-task-" + shortId());
        request.put("description", "Integration test task for LLM trigger.");
        request.put("instructionRichText", "Label the item and use the trigger for assistance.");
        request.put("tags", List.of("integration", "llm-trigger"));
        request.put("quota", 1);
        request.put("deadlineAt", LocalDateTime.now().plusDays(7).toString());
        request.put("overlapCount", 1);
        request.put("publishedTemplateVersionId", templateVersionId);
        request.put("aiProviderId", providerId);
        request.put("aiModelName", aiModel());
        request.put("aiPrompt", """
                You are a LabelHub annotation assistant.
                Always return a JSON object with keys componentId, targetFields, patch, displayText,
                confidence, reasoningSummary, warnings. The patch must only contain the answer field.
                """);
        request.put("aiScoringDimensions", List.of("quality"));
        request.put("aiPassThreshold", new BigDecimal("80.00"));
        request.put("aiManualReviewThreshold", new BigDecimal("60.00"));
        request.put("aiReviewStrategy", "LIGHTWEIGHT");
        request.put("aiFlowPolicy", "MANUAL_FIRST");
        request.put("strategy", "FCFS");
        request.put("reviewLevelCount", 1);
        request.put("rewardRule", Map.of(
                "rewardMode", "APPROVED_ITEM",
                "unitReward", new BigDecimal("1.00"),
                "rewardCurrency", "POINT",
                "rewardVisible", true
        ));
        return number(data(apiClient.post("/api/v1/tasks", request, ownerToken)).get("taskId"));
    }

    private void appendItem(String ownerToken, Long taskId) {
        expectOk(apiClient.post("/api/v1/tasks/" + taskId + "/items/batch-append-json",
                Map.of("items", List.of(Map.of(
                        "externalId", "trigger-item-" + shortId(),
                        "itemJson", Map.of("text", "A support ticket says the user cannot reset their password."),
                        "metadataJson", Map.of("source", "llm-trigger-integration")
                ))),
                ownerToken));
    }

    private Long claimOne(String labelerToken, Long taskId) {
        List<Object> claims = listData(apiClient.post(
                "/api/v1/tasks/" + taskId + "/items/claim",
                Map.of("quantity", 1),
                labelerToken));
        assertThat(claims).hasSize(1);
        return number(map(claims.get(0)).get("assignmentId"));
    }

    private Map<String, Object> awaitCompletedRun(Long triggerRunId, String labelerToken) {
        Map<String, Object> latest = Map.of();
        for (int attempt = 0; attempt < 45; attempt++) {
            llmTaskWorker.poll();
            latest = data(apiClient.get(
                    "/api/v1/llm/triggers/runs/" + triggerRunId,
                    labelerToken));
            String status = String.valueOf(latest.get("status"));
            if (List.of("SUCCESS", "FAILED", "RATE_LIMITED", "MANUAL_REQUIRED").contains(status)) {
                return latest;
            }
            sleep(1000L);
        }
        return latest;
    }

    private String aiApiKey() {
        String value = configuredAiApiKey();
        return "test-dashscope-placeholder".equals(value) ? "" : value;
    }

    private String aiBaseUrl() {
        return System.getProperty("external.ai.base-url",
                "https://dashscope.aliyuncs.com/compatible-mode/v1").trim();
    }

    private String aiModel() {
        return System.getProperty("external.ai.model", "qwen-plus").trim();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> data(ResponseEntity<Map> response) {
        Map<String, Object> body = expectOk(response);
        Object data = body.get("data");
        assertThat(data).isInstanceOf(Map.class);
        return (Map<String, Object>) data;
    }

    @SuppressWarnings("unchecked")
    private List<Object> listData(ResponseEntity<Map> response) {
        Map<String, Object> body = expectOk(response);
        Object data = body.get("data");
        assertThat(data).isInstanceOf(List.class);
        return (List<Object>) data;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> expectOk(ResponseEntity<Map> response) {
        assertThat(response.getStatusCode().is2xxSuccessful())
                .as("response body: %s", response.getBody())
                .isTrue();
        Map<String, Object> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.get("code")).as("response body: %s", body).isEqualTo(0);
        return body;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private List<Object> list(Object value) {
        assertThat(value).isInstanceOf(List.class);
        return (List<Object>) value;
    }

    private Long number(Object value) {
        assertThat(value).isInstanceOf(Number.class);
        return ((Number) value).longValue();
    }

    private String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for LLM trigger result", ex);
        }
    }

    private static String springAiApiKey() {
        return configuredAiApiKey();
    }

    private static String configuredAiApiKey() {
        String value = System.getProperty("external.ai.api-key");
        if (value != null && !value.isBlank()) {
            return value.trim();
        }
        value = System.getenv().getOrDefault("AI_DASHSCOPE_API_KEY", "").trim();
        return value.isBlank() ? "test-dashscope-placeholder" : value;
    }
}
