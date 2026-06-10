package com.labelhub.common.security.ratelimit;

import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Component
public class ApiRateLimitRuleRegistry {

    private static final Duration ONE_MINUTE = Duration.ofMinutes(1);

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final List<ApiRateLimitRule> rules = List.of(
            rule("POST", "/api/v1/auth/login", 10, 0),
            rule("POST", "/api/v1/auth/register", 20, 0),
            rule("POST", "/api/v1/auth/refresh", 60, 0),
            rule("GET", "/api/v1/**/runs/**", 1800, 900),
            rule("GET", "/api/v1/**/status", 1800, 900),
            rule("GET", "/api/v1/**/result", 1800, 900),
            rule("GET", "/api/v1/**/results/**", 1800, 900),
            rule("GET", "/api/v1/**/trace/**", 1800, 900),
            rule("GET", "/api/v1/notifications/**", 1800, 900),
            rule("POST", "/api/v1/**/llm/**", 600, 120),
            rule("POST", "/api/v1/**/pre-annotations/**", 600, 120),
            rule("POST", "/api/v1/**/preannotation/**", 600, 120),
            rule("POST", "/api/v1/**/ai-review/**", 600, 120),
            rule("POST", "/api/v1/**/trigger/**", 600, 120),
            rule("GET", "/api/v1/**", 600, 300),
            rule("*", "/api/v1/**", 300, 120)
    );

    public Optional<ApiRateLimitRule> find(String method, String path) {
        return rules.stream()
                .filter(rule -> methodMatches(rule, method))
                .filter(rule -> pathMatcher.match(rule.pathPattern(), path))
                .max(Comparator.comparingInt(this::specificity));
    }

    private ApiRateLimitRule rule(String method, String pathPattern, long ipRate, long userRate) {
        return new ApiRateLimitRule(method, pathPattern, ipRate, userRate, ONE_MINUTE);
    }

    private boolean methodMatches(ApiRateLimitRule rule, String method) {
        return "*".equals(rule.method()) || rule.method().equalsIgnoreCase(method);
    }

    private int specificity(ApiRateLimitRule rule) {
        return rule.pathPattern().replace("*", "").length() + ("*".equals(rule.method()) ? 0 : 1000);
    }
}
