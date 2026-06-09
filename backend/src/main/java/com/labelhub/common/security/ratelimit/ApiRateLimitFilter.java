package com.labelhub.common.security.ratelimit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.infrastructure.redis.RateLimitResult;
import com.labelhub.infrastructure.redis.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApiRateLimitFilter extends OncePerRequestFilter {

    private final ApiRateLimitRuleRegistry ruleRegistry;
    private final RateLimitService rateLimitService;
    private final ObjectMapper objectMapper;

    public ApiRateLimitFilter(ApiRateLimitRuleRegistry ruleRegistry,
                              RateLimitService rateLimitService,
                              ObjectMapper objectMapper) {
        this.ruleRegistry = ruleRegistry;
        this.rateLimitService = rateLimitService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || !request.getServletPath().startsWith("/api/v1/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String method = request.getMethod();
        String path = request.getServletPath();
        ApiRateLimitRule rule = ruleRegistry.find(method, path).orElse(null);
        if (rule == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (rule.limitsIp()) {
            RateLimitResult ipResult = rateLimitService.tryAcquire(
                    "rate:ip:%s:%s:%s".formatted(clientIp(request), method, normalize(rule.pathPattern())),
                    1,
                    rule.ipRate(),
                    rule.interval());
            if (!ipResult.allowed()) {
                reject(request, response, ipResult);
                return;
            }
        }

        if (rule.limitsUser() && CurrentUserContext.get().isPresent()) {
            Long userId = CurrentUserContext.getUserId();
            RateLimitResult userResult = rateLimitService.tryAcquire(
                    "rate:user:%d:%s:%s".formatted(userId, method, normalize(rule.pathPattern())),
                    1,
                    rule.userRate(),
                    rule.interval());
            if (!userResult.allowed()) {
                reject(request, response, userResult);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private String normalize(String pathPattern) {
        return pathPattern.replace('/', ':').replace("*", "wildcard");
    }

    private void reject(HttpServletRequest request,
                        HttpServletResponse response,
                        RateLimitResult result) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(Math.max(1, result.retryAfterMillis() / 1000)));
        objectMapper.writeValue(response.getWriter(),
                ApiResponse.fail(429001, "请求过于频繁，请稍后重试", request.getHeader("X-Trace-Id")));
    }
}
