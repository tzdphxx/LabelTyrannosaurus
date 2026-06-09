package com.labelhub.common.security.ratelimit;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiRateLimitRuleRegistryTest {

    private final ApiRateLimitRuleRegistry registry = new ApiRateLimitRuleRegistry();

    @Test
    void pollingGetUsesRelaxedLimit() {
        ApiRateLimitRule rule = registry.find("GET", "/api/v1/ai/runs/99/status").orElseThrow();

        assertThat(rule.userRate()).isEqualTo(900);
        assertThat(rule.ipRate()).isEqualTo(1800);
    }

    @Test
    void aiTriggerPostUsesRelaxedWriteLimit() {
        ApiRateLimitRule rule = registry.find("POST", "/api/v1/tasks/7/llm/trigger").orElseThrow();

        assertThat(rule.userRate()).isEqualTo(120);
        assertThat(rule.ipRate()).isEqualTo(600);
    }

    @Test
    void loginOnlyLimitsIp() {
        ApiRateLimitRule rule = registry.find("POST", "/api/v1/auth/login").orElseThrow();

        assertThat(rule.userRate()).isZero();
        assertThat(rule.ipRate()).isEqualTo(10);
    }
}
