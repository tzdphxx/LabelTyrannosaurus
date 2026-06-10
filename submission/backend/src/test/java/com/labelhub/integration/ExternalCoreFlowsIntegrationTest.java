package com.labelhub.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = "labelhub.external-it.enabled", matches = "true")
class ExternalCoreFlowsIntegrationTest {

    private static final String BASE_URL = System.getProperty(
            "labelhub.external-it.base-url",
            "http://127.0.0.1:18080"
    );
    private static final String PASSWORD = "Password123!";
    private static final String NEW_PASSWORD = "Password456!";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Test
    void userProfileAndPasswordWorkflowAgainstRunningService() throws Exception {
        String username = "external-profile-" + shortId();
        User user = register(username, "LABELER");
        String updatedEmail = username + "-updated@example.com";

        assertOk(put("/api/v1/users/me/profile", Map.of(
                "displayName", "External Profile " + shortId(),
                "email", updatedEmail
        ), user.accessToken()));

        Map<String, Object> currentUser = data(get("/api/v1/users/me", user.accessToken()).body());
        assertThat(currentUser.get("username")).isEqualTo(username);
        assertThat(currentUser.get("email")).isEqualTo(updatedEmail);
        assertThat(currentUser.get("role")).isEqualTo("LABELER");

        assertOk(put("/api/v1/users/me/password", Map.of(
                "oldPassword", PASSWORD,
                "newPassword", NEW_PASSWORD
        ), user.accessToken()));

        assertThat(postRaw("/api/v1/auth/login", Map.of(
                "account", username,
                "password", PASSWORD
        ), null).statusCode()).isIn(400, 401);

        Map<String, Object> login = post("/api/v1/auth/login", Map.of(
                "account", username,
                "password", NEW_PASSWORD
        ), null);
        assertOk(login);
        assertThat(data(login).get("accessToken")).isInstanceOf(String.class);
    }

    @Test
    void ownerTemplateLibraryWorkflowAgainstRunningService() throws Exception {
        User owner = register("external-template-" + shortId(), "OWNER");

        Map<String, Object> create = post("/api/v1/owner/templates", Map.of(
                "name", "template-" + shortId(),
                "schemaJson", schema("answer"),
                "changeNote", "initial"
        ), owner.accessToken());
        assertOk(create);
        Map<String, Object> template = data(create);
        Long templateId = number(template.get("templateId"));
        Long versionId = number(map(template.get("currentVersion")).get("versionId"));

        Map<String, Object> list = get("/api/v1/owner/templates", owner.accessToken()).body();
        assertOk(list);
        assertThat(listData(list))
                .anySatisfy(item -> assertThat(number(map(item).get("templateId"))).isEqualTo(templateId));

        Map<String, Object> version = get("/api/v1/template-versions/" + versionId, owner.accessToken()).body();
        assertOk(version);
        assertThat(number(data(version).get("versionId"))).isEqualTo(versionId);

        Map<String, Object> validate = post("/api/v1/schema/validate-answer", Map.of(
                "schemaVersionId", versionId,
                "answerJson", Map.of("answer", "accepted")
        ), owner.accessToken());
        assertOk(validate);
        assertThat(listData(validate)).isEmpty();

        Map<String, Object> fork = post("/api/v1/templates/" + templateId + "/fork", Map.of(
                "baseVersionId", versionId,
                "schemaJson", schema("reviewNote"),
                "changeNote", "fork"
        ), owner.accessToken());
        assertOk(fork);
        assertThat(number(map(data(fork).get("currentVersion")).get("versionId"))).isGreaterThan(versionId);
    }

    @Test
    void roleBoundariesAndProviderCatalogAgainstRunningService() throws Exception {
        User owner = register("external-owner-" + shortId(), "OWNER");
        User labeler = register("external-labeler-" + shortId(), "LABELER");

        Map<String, Object> providers = get("/api/v1/llm-providers", owner.accessToken()).body();
        assertOk(providers);
        assertThat(listData(providers)).isNotEmpty();

        assertThat(get("/api/v1/llm-providers", labeler.accessToken()).statusCode()).isEqualTo(403);
        assertThat(get("/api/v1/users/me", null).statusCode()).isEqualTo(401);
    }

    private User register(String username, String role) throws Exception {
        Map<String, Object> response = post("/api/v1/auth/register", Map.of(
                "username", username,
                "email", username + "@example.com",
                "password", PASSWORD,
                "role", role
        ), null);
        assertOk(response);
        return new User(username, (String) data(response).get("accessToken"));
    }

    private Map<String, Object> schema(String field) {
        return Map.of(
                "components", List.of(Map.of(
                        "type", "Input",
                        "field", field,
                        "required", true
                ))
        );
    }

    private Map<String, Object> post(String path, Map<String, Object> body, String accessToken) throws Exception {
        Response response = postRaw(path, body, accessToken);
        assertThat(response.statusCode())
                .as("POST %s response body: %s", path, response.body())
                .isEqualTo(200);
        return response.body();
    }

    private Response postRaw(String path, Map<String, Object> body, String accessToken) throws Exception {
        return sendWithBody("POST", path, body, accessToken);
    }

    private Map<String, Object> put(String path, Map<String, Object> body, String accessToken) throws Exception {
        Response response = sendWithBody("PUT", path, body, accessToken);
        assertThat(response.statusCode())
                .as("PUT %s response body: %s", path, response.body())
                .isEqualTo(200);
        return response.body();
    }

    private Response sendWithBody(String method, String path, Map<String, Object> body, String accessToken) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(body), StandardCharsets.UTF_8));
        if (accessToken != null) {
            builder.header("Authorization", "Bearer " + accessToken);
        }
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new Response(response.statusCode(), readMap(response.body()));
    }

    private Response get(String path, String accessToken) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri(path))
                .timeout(Duration.ofSeconds(10))
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
