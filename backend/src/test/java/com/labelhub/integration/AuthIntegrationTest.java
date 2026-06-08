package com.labelhub.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.labelhub.integration.support.IntegrationTestBase;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class AuthIntegrationTest extends IntegrationTestBase {

    private static final String PASSWORD = "Password123";

    @Test
    void registerLoginRefreshAndCurrentUserUseRealHttpSecurityFlow() {
        String username = "auth-user-" + shortId();
        Map<String, Object> registerRequest = Map.of(
                "username", username,
                "email", username + "@example.com",
                "password", PASSWORD,
                "role", "LABELER"
        );

        ResponseEntity<Map> registerResponse = apiClient.post("/api/v1/auth/register", registerRequest);

        assertOk(registerResponse);
        Map<String, Object> registerData = data(registerResponse);
        assertThat(registerData.get("accessToken")).isInstanceOf(String.class);
        assertThat((String) registerData.get("accessToken")).isNotBlank();
        assertThat(registerData.get("refreshToken")).isInstanceOf(String.class);
        assertThat((String) registerData.get("refreshToken")).isNotBlank();
        assertThat(registerData.get("tokenVersion")).isEqualTo(1);
        assertThat(registerData.get("role")).isEqualTo("LABELER");

        ResponseEntity<Map> loginResponse = apiClient.post("/api/v1/auth/login", Map.of(
                "account", username,
                "password", PASSWORD
        ));

        assertOk(loginResponse);
        Map<String, Object> loginData = data(loginResponse);
        assertThat((String) loginData.get("accessToken")).isNotBlank();
        assertThat((String) loginData.get("refreshToken")).isNotBlank();
        assertThat(loginData.get("tokenVersion")).isEqualTo(1);
        assertThat(loginData.get("role")).isEqualTo("LABELER");

        ResponseEntity<Map> refreshResponse = apiClient.post("/api/v1/auth/refresh", Map.of(
                "refreshToken", loginData.get("refreshToken")
        ));

        assertOk(refreshResponse);
        Map<String, Object> refreshData = data(refreshResponse);
        assertThat((String) refreshData.get("accessToken")).isNotBlank();
        assertThat((String) refreshData.get("refreshToken")).isNotBlank();
        assertThat(refreshData.get("tokenVersion")).isEqualTo(1);
        assertThat(refreshData.get("role")).isEqualTo("LABELER");

        ResponseEntity<Map> currentUserResponse = apiClient.get(
                "/api/v1/users/me",
                (String) refreshData.get("accessToken")
        );

        assertOk(currentUserResponse);
        Map<String, Object> currentUser = data(currentUserResponse);
        assertThat(currentUser.get("username")).isEqualTo(username);
        assertThat(currentUser.get("email")).isEqualTo(username + "@example.com");
        assertThat(currentUser.get("role")).isEqualTo("LABELER");
    }

    @Test
    void missingTokenIsRejectedForProtectedEndpoint() {
        ResponseEntity<Map> response = apiClient.get("/api/v1/users/me");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        Map<String, Object> body = body(response);
        assertThat(body.get("data")).isNull();
        assertThat(body.get("code")).isInstanceOf(Number.class);
    }

    @Test
    void labelerCannotAccessOwnerOnlyLlmProvidersEndpoint() {
        var labeler = fixtures.register("labeler-authz", "LABELER");

        ResponseEntity<Map> response = apiClient.get("/api/v1/llm-providers", labeler.accessToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        Map<String, Object> body = body(response);
        assertThat(body.get("data")).isNull();
        assertThat(body.get("code")).isEqualTo(403001);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> data(ResponseEntity<Map> response) {
        Object data = body(response).get("data");
        assertThat(data).isInstanceOf(Map.class);
        return (Map<String, Object>) data;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> body(ResponseEntity<Map> response) {
        assertThat(response.getBody()).isNotNull();
        return (Map<String, Object>) response.getBody();
    }

    private void assertOk(ResponseEntity<Map> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(body(response).get("code")).isEqualTo(0);
    }

    private String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
