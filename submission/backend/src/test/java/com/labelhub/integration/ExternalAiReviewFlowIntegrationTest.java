package com.labelhub.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
class ExternalAiReviewFlowIntegrationTest extends IntegrationTestBase {

    @Autowired
    private LlmApiKeyEncryptor apiKeyEncryptor;

    @DynamicPropertySource
    static void dashscopeProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.ai.dashscope.api-key", ExternalAiReviewFlowIntegrationTest::springAiApiKey);
        registry.add("spring.ai.dashscope.agent.api-key", ExternalAiReviewFlowIntegrationTest::springAiApiKey);
    }

    @Test
    void labelerSubmitTriggersAsyncAiReviewAgainstDashscope() {
        String apiKey = aiApiKey();
        assumeTrue(!apiKey.isBlank(), "set -Dexternal.ai.api-key or AI_DASHSCOPE_API_KEY");

        TestFixtures.RegisteredUser owner = fixtures.register("ai-review-owner", "OWNER");
        TestFixtures.RegisteredUser labeler = fixtures.register("ai-review-labeler", "LABELER");
        TestFixtures.CreatedTemplate template = fixtures.createTemplate(owner.accessToken());
        Long providerId = createDashscopeProvider(apiKey);
        Long taskId = createTask(owner.accessToken(), template.templateVersionId(), providerId);
        appendItem(owner.accessToken(), taskId);
        expectOk(apiClient.post("/api/v1/tasks/" + taskId + "/publish", Map.of(), owner.accessToken()));

        Map<String, Object> claim = claimOne(labeler.accessToken(), taskId);
        Long assignmentId = number(claim.get("assignmentId"));
        Integer draftVersion = ((Number) claim.get("draftVersion")).intValue();

        Map<String, Object> submission = data(apiClient.post(
                "/api/v1/claims/" + assignmentId + "/submit",
                Map.of(
                        "answerJson", "{\"answer\":\"The user cannot reset their password and needs password reset guidance.\"}",
                        "clientVersion", draftVersion
                ),
                labeler.accessToken()));
        Long submissionId = number(submission.get("submissionId"));
        assertThat(String.valueOf(submission.get("status"))).isEqualTo("AI_REVIEWING");
        assertThat(number(submission.get("agentRunId"))).isNotNull();

        Map<String, Object> result = awaitAiReviewResult(submissionId, owner.accessToken());
        assertThat(String.valueOf(result.get("status")))
                .as("AI review result: %s", result)
                .isEqualTo("SUCCESS");
        assertThat(String.valueOf(result.get("decision"))).isIn("PASS", "REJECT", "UNCERTAIN");
        assertThat(String.valueOf(result.get("modelName"))).isEqualTo(aiModel());
        assertThat(result.get("providerId")).isNotNull();
        assertThat(result.get("averageScore")).isNotNull();
        assertThat(result.get("confidence")).isNotNull();
        assertThat(map(result.get("dimensionScores"))).containsKey("quality");

        Map<String, Object> persisted = jdbcTemplate.queryForMap("""
                SELECT status, decision, model_name, provider_id, average_score, confidence
                FROM ai_review_results
                WHERE submission_id = ?
                """, submissionId);
        assertThat(String.valueOf(persisted.get("status"))).isEqualTo("SUCCESS");
        assertThat(String.valueOf(persisted.get("decision"))).isEqualTo(String.valueOf(result.get("decision")));
    }

    @Test
    void failedAiReviewCanBeRetriedThroughExistingApiAndPrintsLlmAnswer() {
        String apiKey = aiApiKey();
        assumeTrue(!apiKey.isBlank(), "set -Dexternal.ai.api-key or AI_DASHSCOPE_API_KEY");

        TestFixtures.RegisteredUser owner = fixtures.register("ai-retry-owner", "OWNER");
        TestFixtures.RegisteredUser labeler = fixtures.register("ai-retry-labeler", "LABELER");
        TestFixtures.RegisteredUser reviewer = createReviewer("ai-retry-reviewer");
        TestFixtures.CreatedTemplate template = fixtures.createTemplate(owner.accessToken());
        Long providerId = createBrokenDashscopeProvider();
        Long taskId = createTask(owner.accessToken(), template.templateVersionId(), providerId);
        appendItem(owner.accessToken(), taskId);
        expectOk(apiClient.post("/api/v1/tasks/" + taskId + "/publish", Map.of(), owner.accessToken()));

        Map<String, Object> claim = claimOne(labeler.accessToken(), taskId);
        Long assignmentId = number(claim.get("assignmentId"));
        Integer draftVersion = ((Number) claim.get("draftVersion")).intValue();
        Map<String, Object> submission = data(apiClient.post(
                "/api/v1/claims/" + assignmentId + "/submit",
                Map.of(
                        "answerJson", "{\"answer\":\"The user cannot reset their password and needs password reset guidance.\"}",
                        "clientVersion", draftVersion
                ),
                labeler.accessToken()));
        Long submissionId = number(submission.get("submissionId"));

        Map<String, Object> failed = awaitAiReviewResult(submissionId, owner.accessToken());
        assertThat(String.valueOf(failed.get("status")))
                .as("initial failed AI review result: %s", failed)
                .isEqualTo("FAILED");

        updateProviderApiKey(providerId, apiKey);
        Map<String, Object> retryResponse = data(apiClient.post(
                "/api/v1/submissions/" + submissionId + "/ai-review/retry",
                Map.of(),
                reviewer.accessToken()));
        assertThat(String.valueOf(retryResponse.get("status"))).isEqualTo("FAILED");

        Map<String, Object> retried = awaitAiReviewRetrySuccess(submissionId, owner.accessToken());
        assertThat(String.valueOf(retried.get("status")))
                .as("retried AI review result: %s", retried)
                .isEqualTo("SUCCESS");
        assertThat(String.valueOf(retried.get("decision"))).isIn("PASS", "REJECT", "UNCERTAIN");

        Map<String, Object> persisted = jdbcTemplate.queryForMap("""
                SELECT status, decision, average_score, confidence, dimension_scores, suggestion, raw_response
                FROM ai_review_results
                WHERE submission_id = ?
                """, submissionId);
        System.out.println("AI_REVIEW_RETRY_RESULT=" + persisted);
        System.out.println("AI_REVIEW_LLM_RAW_RESPONSE=" + persisted.get("raw_response"));
    }

    private Long createDashscopeProvider(String apiKey) {
        String providerCode = "dashscope-ai-review-" + shortId();
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
                "DashScope AI Review Integration",
                aiBaseUrl(),
                apiKeyEncryptor.encrypt(apiKey),
                aiModel(),
                "{}");
        return jdbcTemplate.queryForObject(
                "SELECT id FROM llm_providers WHERE provider_code = ?",
                Long.class,
                providerCode);
    }

    private Long createBrokenDashscopeProvider() {
        String providerCode = "dashscope-ai-review-broken-" + shortId();
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
                "Broken DashScope AI Review Integration",
                aiBaseUrl(),
                "not-an-encrypted-api-key",
                aiModel(),
                "{}");
        return jdbcTemplate.queryForObject(
                "SELECT id FROM llm_providers WHERE provider_code = ?",
                Long.class,
                providerCode);
    }

    private void updateProviderApiKey(Long providerId, String apiKey) {
        jdbcTemplate.update("""
                UPDATE llm_providers
                SET encrypted_api_key = ?, base_url = ?, default_model = ?
                WHERE id = ?
                """,
                apiKeyEncryptor.encrypt(apiKey),
                aiBaseUrl(),
                aiModel(),
                providerId);
    }

    private Long createTask(String ownerToken, Long templateVersionId, Long providerId) {
        Map<String, Object> request = new java.util.LinkedHashMap<>();
        request.put("title", "ai-review-task-" + shortId());
        request.put("description", "Integration test task for async AI review.");
        request.put("instructionRichText", "Review the support ticket answer for correctness and usefulness.");
        request.put("tags", List.of("integration", "ai-review"));
        request.put("quota", 1);
        request.put("deadlineAt", LocalDateTime.now().plusDays(7).toString());
        request.put("overlapCount", 1);
        request.put("publishedTemplateVersionId", templateVersionId);
        request.put("aiProviderId", providerId);
        request.put("aiModelName", aiModel());
        request.put("aiPrompt", """
                You are a strict LabelHub AI reviewer.
                Return only one JSON object with exactly these fields:
                decision: one of PASS, REJECT, UNCERTAIN;
                averageScore: a number from 0 to 100;
                dimensionScores: an object containing quality as a number from 0 to 100;
                confidence: a number from 0 to 1;
                riskFlags: an array of strings, empty when there is no risk;
                suggestion: one concise English sentence.
                For a reasonable password reset support answer, return PASS with quality above 90 and no risk flags.
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
                        "externalId", "ai-review-item-" + shortId(),
                        "itemJson", Map.of("text", "A support ticket says the user cannot reset their password."),
                        "metadataJson", Map.of("source", "ai-review-integration")
                ))),
                ownerToken));
    }

    private Map<String, Object> claimOne(String labelerToken, Long taskId) {
        List<Object> claims = listData(apiClient.post(
                "/api/v1/tasks/" + taskId + "/items/claim",
                Map.of("quantity", 1),
                labelerToken));
        assertThat(claims).hasSize(1);
        return map(claims.get(0));
    }

    private Map<String, Object> awaitAiReviewResult(Long submissionId, String ownerToken) {
        Map<String, Object> latest = Map.of();
        for (int attempt = 0; attempt < 75; attempt++) {
            ResponseEntity<Map> response = apiClient.get(
                    "/api/v1/submissions/" + submissionId + "/ai-review-result",
                    ownerToken);
            if (response.getStatusCode().is2xxSuccessful()
                    && response.getBody() != null
                    && Integer.valueOf(0).equals(response.getBody().get("code"))) {
                latest = data(response);
                String status = String.valueOf(latest.get("status"));
                if (List.of("SUCCESS", "FAILED", "RATE_LIMITED", "MANUAL_REQUIRED").contains(status)) {
                    return latest;
                }
            }
            sleep(1000L);
        }
        return latest;
    }

    private Map<String, Object> awaitAiReviewRetrySuccess(Long submissionId, String ownerToken) {
        Map<String, Object> latest = Map.of();
        for (int attempt = 0; attempt < 75; attempt++) {
            ResponseEntity<Map> response = apiClient.get(
                    "/api/v1/submissions/" + submissionId + "/ai-review-result",
                    ownerToken);
            if (response.getStatusCode().is2xxSuccessful()
                    && response.getBody() != null
                    && Integer.valueOf(0).equals(response.getBody().get("code"))) {
                latest = data(response);
                if ("SUCCESS".equals(String.valueOf(latest.get("status")))) {
                    return latest;
                }
            }
            sleep(1000L);
        }
        return latest;
    }

    private TestFixtures.RegisteredUser createReviewer(String usernamePrefix) {
        TestFixtures.RegisteredUser user = fixtures.register(usernamePrefix, "OWNER");
        jdbcTemplate.update("""
                UPDATE user_roles ur
                INNER JOIN users u ON u.id = ur.user_id
                SET ur.role_code = 'REVIEWER'
                WHERE u.username = ?
                """, user.username());
        Map<String, Object> data = data(apiClient.post("/api/v1/auth/login", Map.of(
                "account", user.username(),
                "password", "Password123"
        )));
        return new TestFixtures.RegisteredUser(
                user.username(),
                String.valueOf(data.get("accessToken")),
                String.valueOf(data.get("refreshToken")),
                ((Number) data.get("tokenVersion")).intValue(),
                String.valueOf(data.get("role"))
        );
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
            throw new IllegalStateException("Interrupted while waiting for AI review result", ex);
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
