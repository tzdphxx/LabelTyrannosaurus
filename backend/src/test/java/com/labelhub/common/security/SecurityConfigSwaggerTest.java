package com.labelhub.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class SecurityConfigSwaggerTest {

    @Test
    void enablesSwaggerOnlyForLocalAndDevProfiles() {
        assertThat(SecurityConfig.isSwaggerProfile(new MockEnvironment().withProperty("spring.profiles.active", "local")))
                .isTrue();
        assertThat(SecurityConfig.isSwaggerProfile(new MockEnvironment().withProperty("spring.profiles.active", "dev")))
                .isTrue();
        assertThat(SecurityConfig.isSwaggerProfile(new MockEnvironment().withProperty("spring.profiles.active", "prod")))
                .isFalse();
        assertThat(SecurityConfig.isSwaggerProfile(new MockEnvironment()))
                .isFalse();
    }

    @Test
    void includesKnife4jDocumentPaths() {
        assertThat(SecurityConfig.swaggerPaths())
                .contains("/doc.html", "/webjars/**", "/v3/api-docs/**");
    }
}
