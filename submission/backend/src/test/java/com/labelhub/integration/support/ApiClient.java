package com.labelhub.integration.support;

import java.util.Map;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.test.web.client.TestRestTemplate;

public class ApiClient {

    private final TestRestTemplate restTemplate;
    private final String baseUrl;

    public ApiClient(TestRestTemplate restTemplate, String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public ResponseEntity<Map> get(String path) {
        return get(path, null);
    }

    public ResponseEntity<Map> get(String path, String bearerToken) {
        return exchange(path, HttpMethod.GET, null, bearerToken);
    }

    public ResponseEntity<Map> post(String path, Object body) {
        return post(path, body, null);
    }

    public ResponseEntity<Map> post(String path, Object body, String bearerToken) {
        return exchange(path, HttpMethod.POST, body, bearerToken);
    }

    public ResponseEntity<Map> put(String path, Object body) {
        return put(path, body, null);
    }

    public ResponseEntity<Map> put(String path, Object body, String bearerToken) {
        return exchange(path, HttpMethod.PUT, body, bearerToken);
    }

    public ResponseEntity<Map> delete(String path) {
        return delete(path, null);
    }

    public ResponseEntity<Map> delete(String path, String bearerToken) {
        return exchange(path, HttpMethod.DELETE, null, bearerToken);
    }

    private ResponseEntity<Map> exchange(String path, HttpMethod method, Object body, String bearerToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        if (bearerToken != null && !bearerToken.isBlank()) {
            headers.setBearerAuth(bearerToken);
        }
        return restTemplate.exchange(url(path), method, new HttpEntity<>(body, headers), Map.class);
    }

    private String url(String path) {
        if (path == null || path.isBlank()) {
            return baseUrl;
        }
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path;
        }
        return baseUrl + (path.startsWith("/") ? path : "/" + path);
    }
}
