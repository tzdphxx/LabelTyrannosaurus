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
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

@EnabledIfSystemProperty(named = "labelhub.external-it.enabled", matches = "true")
class ExternalAuthSmokeIntegrationTest {

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
    void registerLoginRefreshAndCurrentUserAgainstRunningService() throws Exception {
        String username = "external-it-" + shortId();

        Map<String, Object> register = post("/api/v1/auth/register", Map.of(
                "username", username,
                "email", username + "@example.com",
                "password", PASSWORD,
                "role", "LABELER"
        ));

        assertOk(register);

        Map<String, Object> login = post("/api/v1/auth/login", Map.of(
                "account", username,
                "password", PASSWORD
        ));

        assertOk(login);
        Map<String, Object> loginData = data(login);

        Map<String, Object> refresh = post("/api/v1/auth/refresh", Map.of(
                "refreshToken", loginData.get("refreshToken")
        ));

        assertOk(refresh);
        Map<String, Object> refreshData = data(refresh);

        Map<String, Object> currentUser = get(
                "/api/v1/users/me",
                (String) refreshData.get("accessToken")
        ).body();

        assertOk(currentUser);
        Map<String, Object> currentUserData = data(currentUser);
        assertThat(currentUserData.get("username")).isEqualTo(username);
        assertThat(currentUserData.get("email")).isEqualTo(username + "@example.com");
        assertThat(currentUserData.get("role")).isEqualTo("LABELER");

        assertThat(get("/api/v1/users/me", null).statusCode()).isEqualTo(401);
    }

    @Test
    void labelerCannotAccessOwnerOnlyLlmProvidersAgainstRunningService() throws Exception {
        String username = "external-it-authz-" + shortId();
        Map<String, Object> register = post("/api/v1/auth/register", Map.of(
                "username", username,
                "email", username + "@example.com",
                "password", PASSWORD,
                "role", "LABELER"
        ));

        assertOk(register);
        String accessToken = (String) data(register).get("accessToken");

        assertThat(get("/api/v1/llm-providers", accessToken).statusCode()).isEqualTo(403);
    }

    private Map<String, Object> post(String path, Map<String, Object> body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(OBJECT_MAPPER.writeValueAsString(body), StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        assertThat(response.statusCode()).isEqualTo(200);
        return readMap(response.body());
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

    private void assertOk(Map<String, Object> response) {
        assertThat(response.get("code")).isEqualTo(0);
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

    private record Response(int statusCode, Map<String, Object> body) {
    }
}
