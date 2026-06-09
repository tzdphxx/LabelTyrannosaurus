package com.labelhub.integration.support;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

public class TestFixtures {

    private static final String PASSWORD = "Password123";

    private final ApiClient apiClient;
    private final JdbcTemplate jdbcTemplate;

    public TestFixtures(ApiClient apiClient, JdbcTemplate jdbcTemplate) {
        this.apiClient = apiClient;
        this.jdbcTemplate = jdbcTemplate;
    }

    public RegisteredUser register(String username, String role) {
        String uniqueUsername = username + "-" + shortId();
        Map<String, Object> request = Map.of(
                "username", uniqueUsername,
                "email", uniqueUsername + "@example.com",
                "password", PASSWORD,
                "role", role
        );
        Map<String, Object> data = data(apiClient.post("/api/v1/auth/register", request));
        return new RegisteredUser(
                uniqueUsername,
                (String) data.get("accessToken"),
                (String) data.get("refreshToken"),
                ((Number) data.get("tokenVersion")).intValue(),
                String.valueOf(data.get("role"))
        );
    }

    public CreatedTemplate createTemplate(String ownerToken) {
        Map<String, Object> request = Map.of(
                "name", "template-" + shortId(),
                "schemaJson", Map.of(
                        "components", List.of(Map.of(
                                "type", "Input",
                                "field", "answer",
                                "required", true
                        ))
                ),
                "changeNote", "integration test fixture"
        );
        Map<String, Object> data = data(apiClient.post("/api/v1/owner/templates", request, ownerToken));
        Map<String, Object> currentVersion = map(data.get("currentVersion"));
        return new CreatedTemplate(
                number(data.get("templateId")),
                number(currentVersion.get("versionId"))
        );
    }

    public CreatedTask createTask(String ownerToken, Long templateVersionId) {
        Long providerId = ensureLlmProvider();
        Map<String, Object> request = new java.util.LinkedHashMap<>();
        request.put("title", "task-" + shortId());
        request.put("description", "integration test fixture");
        request.put("instructionRichText", "Please label the item.");
        request.put("tags", List.of("integration"));
        request.put("quota", 10);
        request.put("deadlineAt", LocalDateTime.now().plusDays(7).toString());
        request.put("overlapCount", 1);
        request.put("publishedTemplateVersionId", templateVersionId);
        request.put("aiProviderId", providerId);
        request.put("aiPrompt", "Return a JSON review result for the submitted answer.");
        request.put("aiScoringDimensions", List.of("quality"));
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

        Map<String, Object> data = data(apiClient.post("/api/v1/tasks", request, ownerToken));
        return new CreatedTask(number(data.get("taskId")), String.valueOf(data.get("status")));
    }

    public PublishedTask createPublishedTaskWithItems(String ownerToken, int itemCount) {
        CreatedTemplate template = createTemplate(ownerToken);
        CreatedTask task = createTask(ownerToken, template.templateVersionId());
        appendItems(ownerToken, task.taskId(), itemCount);
        Map<String, Object> data = data(apiClient.post("/api/v1/tasks/" + task.taskId() + "/publish", Map.of(), ownerToken));
        return new PublishedTask(
                task.taskId(),
                template.templateId(),
                template.templateVersionId(),
                String.valueOf(data.get("status")),
                itemCount
        );
    }

    private void appendItems(String ownerToken, Long taskId, int itemCount) {
        if (itemCount < 1) {
            throw new IllegalArgumentException("itemCount must be at least 1");
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 0; i < itemCount; i++) {
            items.add(Map.of(
                    "externalId", "item-" + shortId() + "-" + i,
                    "itemJson", Map.of("text", "item " + i),
                "metadataJson", Map.of("source", "integration-test")
            ));
        }
        expectOk(apiClient.post("/api/v1/tasks/" + taskId + "/items/batch-append-json", Map.of("items", items), ownerToken));
    }

    private Long ensureLlmProvider() {
        List<Long> existing = jdbcTemplate.queryForList(
                "SELECT id FROM llm_providers WHERE provider_code = ?",
                Long.class,
                "integration-test"
        );
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        jdbcTemplate.update("""
                INSERT INTO llm_providers (
                    provider_code, provider_name, base_url, encrypted_api_key, default_model,
                    custom_headers_json, enabled, platform_rate_limit_per_minute,
                    task_rate_limit_per_minute, user_rate_limit_per_minute, support_vision,
                    support_multi_image, max_image_count, vision_model, structured_output_mode
                )
                VALUES (?, ?, ?, ?, ?, CAST(? AS JSON), 1, NULL, NULL, NULL, 0, 0, 10, NULL, 'NONE')
                """,
                "integration-test",
                "Integration Test Provider",
                "http://localhost/llm-not-called",
                "integration-test-key",
                "integration-test-model",
                "{}"
        );
        return jdbcTemplate.queryForObject(
                "SELECT id FROM llm_providers WHERE provider_code = ?",
                Long.class,
                "integration-test"
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> data(ResponseEntity<Map> response) {
        Map<String, Object> body = expectOk(response);
        Object data = body.get("data");
        if (!(data instanceof Map<?, ?> map)) {
            throw new IllegalStateException("Expected object data but got " + data);
        }
        return (Map<String, Object>) map;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> expectOk(ResponseEntity<Map> response) {
        Map<String, Object> body = response.getBody();
        if (body == null) {
            throw new IllegalStateException("Expected response body but got none");
        }
        Object code = body.get("code");
        if (!(code instanceof Number number) || number.intValue() != 0) {
            throw new IllegalStateException("Expected successful ApiResponse but got " + body);
        }
        return (Map<String, Object>) body;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    private Long number(Object value) {
        return ((Number) value).longValue();
    }

    private String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public record RegisteredUser(String username, String accessToken, String refreshToken, int tokenVersion, String role) {
    }

    public record CreatedTemplate(Long templateId, Long templateVersionId) {
    }

    public record CreatedTask(Long taskId, String status) {
    }

    public record PublishedTask(Long taskId, Long templateId, Long templateVersionId, String status, int itemCount) {
    }
}
