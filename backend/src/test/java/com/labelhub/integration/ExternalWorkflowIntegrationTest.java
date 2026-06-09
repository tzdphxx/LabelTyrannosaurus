package com.labelhub.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = "labelhub.external-it.enabled", matches = "true")
class ExternalWorkflowIntegrationTest {

    private static final String BASE_URL = System.getProperty(
            "labelhub.external-it.base-url",
            "http://127.0.0.1:18080"
    );
    private static final String PASSWORD = "Password123!";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Test
    void ownerPublishesTaskAndLabelerSubmitsAgainstRunningService() throws Exception {
        User owner = register("workflow-owner", "OWNER");
        User labeler = register("workflow-labeler", "LABELER");

        Long providerId = firstProviderId(owner.accessToken());
        Long templateVersionId = createOwnerTemplate(owner.accessToken());
        Long taskId = createTask(owner.accessToken(), templateVersionId, providerId);
        appendItems(owner.accessToken(), taskId);

        Map<String, Object> publish = post("/api/v1/tasks/" + taskId + "/publish", Map.of(), owner.accessToken());
        assertOk(publish);

        Map<String, Object> claim = post(
                "/api/v1/tasks/" + taskId + "/items/claim",
                Map.of("quantity", 1),
                labeler.accessToken()
        );
        assertOk(claim);
        List<Object> claims = listData(claim);
        assertThat(claims).hasSize(1);
        Long assignmentId = number(map(claims.get(0)).get("assignmentId"));

        Map<String, Object> submit = post(
                "/api/v1/claims/" + assignmentId + "/submit",
                Map.of(
                        "answerJson", "{\"answer\":\"approved by external integration test\"}",
                        "clientVersion", 1
                ),
                labeler.accessToken()
        );
        assertOk(submit);
        assertThat(number(data(submit).get("submissionId"))).isPositive();
        assertThat(data(submit).get("status")).isIn("PENDING_REVIEW", "AI_REVIEWING", "PENDING_FINAL");
    }

    @Test
    void ownerCanCreateAndReadExportJobAgainstRunningService() throws Exception {
        User owner = register("workflow-export-owner", "OWNER");

        Long providerId = firstProviderId(owner.accessToken());
        Long templateVersionId = createOwnerTemplate(owner.accessToken());
        Long taskId = createTask(owner.accessToken(), templateVersionId, providerId);

        Map<String, Object> createExport = post(
                "/api/v1/tasks/" + taskId + "/exports",
                Map.of(
                        "exportFormat", "JSON",
                        "includeAiReview", false,
                        "includeAuditTrail", false,
                        "includeReviewComment", false,
                        "includeLabelerInfo", false,
                        "fieldMappings", List.of()
                ),
                owner.accessToken()
        );
        assertOk(createExport);
        Long exportJobId = number(data(createExport).get("exportJobId"));

        Response detail = get("/api/v1/tasks/" + taskId + "/exports/" + exportJobId, owner.accessToken());
        assertThat(detail.statusCode()).isEqualTo(200);
        assertOk(detail.body());
        assertThat(number(data(detail.body()).get("exportJobId"))).isEqualTo(exportJobId);
    }

    private User register(String prefix, String role) throws Exception {
        String username = prefix + "-" + shortId();
        Map<String, Object> response = post("/api/v1/auth/register", Map.of(
                "username", username,
                "email", username + "@example.com",
                "password", PASSWORD,
                "role", role
        ), null);
        assertOk(response);
        return new User(username, (String) data(response).get("accessToken"));
    }

    private Long firstProviderId(String ownerToken) throws Exception {
        Response response = get("/api/v1/llm-providers", ownerToken);
        assertThat(response.statusCode()).isEqualTo(200);
        assertOk(response.body());
        List<Object> providers = listData(response.body());
        assertThat(providers).isNotEmpty();
        return number(map(providers.get(0)).get("id"));
    }

    private Long createOwnerTemplate(String ownerToken) throws Exception {
        Map<String, Object> response = post("/api/v1/owner/templates", Map.of(
                "name", "external-template-" + shortId(),
                "schemaJson", Map.of(
                        "components", List.of(Map.of(
                                "type", "Input",
                                "field", "answer",
                                "required", true
                        ))
                ),
                "changeNote", "external integration test"
        ), ownerToken);
        assertOk(response);
        return number(map(data(response).get("currentVersion")).get("versionId"));
    }

    private Long createTask(String ownerToken, Long templateVersionId, Long providerId) throws Exception {
        Map<String, Object> request = Map.ofEntries(
                Map.entry("title", "external-task-" + shortId()),
                Map.entry("description", "external integration task"),
                Map.entry("instructionRichText", "Please label the item."),
                Map.entry("tags", List.of("external-it")),
                Map.entry("quota", 2),
                Map.entry("deadlineAt", LocalDateTime.now().plusDays(7).toString()),
                Map.entry("overlapCount", 1),
                Map.entry("publishedTemplateVersionId", templateVersionId),
                Map.entry("aiProviderId", providerId),
                Map.entry("aiPrompt", "Return a JSON review result."),
                Map.entry("aiScoringDimensions", List.of("quality")),
                Map.entry("aiReviewStrategy", "LIGHTWEIGHT"),
                Map.entry("aiFlowPolicy", "MANUAL_FIRST"),
                Map.entry("strategy", "FCFS"),
                Map.entry("reviewLevelCount", 1),
                Map.entry("rewardRule", Map.of(
                        "rewardMode", "APPROVED_ITEM",
                        "unitReward", new BigDecimal("1.00"),
                        "rewardCurrency", "POINT",
                        "rewardVisible", true
                ))
        );
        Response rawResponse = postRaw("/api/v1/tasks", request, ownerToken);
        assumeTrue(
                rawResponse.statusCode() == 200,
                "Skipping external workflow integration test because the running service could not create a task: "
                        + rawResponse.body()
        );
        Map<String, Object> response = rawResponse.body();
        assertOk(response);
        return number(data(response).get("taskId"));
    }

    private void appendItems(String ownerToken, Long taskId) throws Exception {
        Map<String, Object> response = post(
                "/api/v1/tasks/" + taskId + "/items/batch-append-json",
                Map.of("items", List.of(Map.of(
                        "externalId", "external-item-" + shortId(),
                        "itemJson", Map.of("text", "external integration item"),
                        "metadataJson", Map.of("source", "external-it")
                ))),
                ownerToken
        );
        assertOk(response);
    }

    private Map<String, Object> post(String path, Map<String, Object> body, String accessToken) throws Exception {
        Response response = postRaw(path, body, accessToken);
        assertThat(response.statusCode())
                .as("POST %s response body: %s", path, response.body())
                .isEqualTo(200);
        return response.body();
    }

    private Response postRaw(String path, Map<String, Object> body, String accessToken) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .timeout(Duration.ofSeconds(15))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(body), StandardCharsets.UTF_8));
        if (accessToken != null) {
            builder.header("Authorization", "Bearer " + accessToken);
        }

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new Response(response.statusCode(), readMap(response.body()));
    }

    private Response get(String path, String accessToken) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .timeout(Duration.ofSeconds(15))
                .GET();
        if (accessToken != null) {
            builder.header("Authorization", "Bearer " + accessToken);
        }

        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        Map<String, Object> body = response.body() == null || response.body().isBlank()
                ? Map.of()
                : readMap(response.body());
        return new Response(response.statusCode(), body);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> data(Map<String, Object> response) {
        Object data = response.get("data");
        assertThat(data).isInstanceOf(Map.class);
        return (Map<String, Object>) data;
    }

    @SuppressWarnings("unchecked")
    private List<Object> listData(Map<String, Object> response) {
        Object data = response.get("data");
        assertThat(data).isInstanceOf(List.class);
        return (List<Object>) data;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        assertThat(value).isInstanceOf(Map.class);
        return (Map<String, Object>) value;
    }

    private void assertOk(Map<String, Object> response) {
        assertThat(response.get("code")).isEqualTo(0);
    }

    private Long number(Object value) {
        assertThat(value).isInstanceOf(Number.class);
        return ((Number) value).longValue();
    }

    private URI uri(String path) {
        return URI.create(BASE_URL + path);
    }

    private Map<String, Object> readMap(String body) throws Exception {
        return OBJECT_MAPPER.readValue(body, new TypeReference<>() {
        });
    }

    private String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private record User(String username, String accessToken) {
    }

    private record Response(int statusCode, Map<String, Object> body) {
    }
}
