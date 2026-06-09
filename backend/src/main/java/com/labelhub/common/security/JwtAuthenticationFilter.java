package com.labelhub.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenService jwtTokenService;
    private final AuthUserCacheService authUserCacheService;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService, AuthUserCacheService authUserCacheService) {
        this.jwtTokenService = jwtTokenService;
        this.authUserCacheService = authUserCacheService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = extractToken(request);
            if (token != null) {
                try {
                    authenticate(token);
                } catch (Exception ignored) {
                    SecurityContextHolder.clearContext();
                    CurrentUserContext.clear();
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            CurrentUserContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private String extractToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        String tokenParam = request.getParameter("token");
        if (tokenParam != null && !tokenParam.isBlank()) {
            return tokenParam;
        }
        return null;
    }

    private void authenticate(String token) {
        JwtTokenService.TokenClaims claims = jwtTokenService.parseAccessToken(token);
        CurrentUser currentUser = authUserCacheService.authenticate(claims).orElse(null);
        if (currentUser == null) {
            return;
        }
        CurrentUserContext.set(currentUser);
        var authorities = currentUser.roles().stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, null, authorities)
        );
    }
}
