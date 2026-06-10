package com.labelhub.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

class SecurityConfigCorsTest {

    @Test
    void allowsAllOriginsHeadersAndCommonMethodsForTemporaryFrontendDebugging() {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/auth/login");
        request.addHeader("Origin", "http://192.168.1.25:5173");

        CorsConfiguration configuration = new SecurityConfig()
                .corsConfigurationSource()
                .getCorsConfiguration(request);

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins()).containsExactly("*");
        assertThat(configuration.getAllowedHeaders()).containsExactly("*");
        assertThat(configuration.getAllowedMethods())
                .containsExactlyElementsOf(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        assertThat(configuration.getExposedHeaders()).containsExactly("X-Trace-Id");
        assertThat(configuration.getAllowCredentials()).isFalse();
    }
}
