package com.labelhub.common.security;

import com.labelhub.modules.auth.domain.UserEntity;
import com.labelhub.modules.auth.repository.UserMapper;
import com.labelhub.modules.auth.repository.UserRoleMapper;
import com.labelhub.common.exception.BusinessException;
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
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService, UserMapper userMapper, UserRoleMapper userRoleMapper) {
        this.jwtTokenService = jwtTokenService;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authorization != null && authorization.startsWith("Bearer ")) {
                try {
                    authenticate(authorization.substring(7));
                } catch (BusinessException ignored) {
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

    private void authenticate(String token) {
        JwtTokenService.TokenClaims claims = jwtTokenService.parseAccessToken(token);
        UserEntity user = userMapper.selectById(claims.userId());
        if (user == null || !Boolean.TRUE.equals(user.getEnabled()) || !claims.tokenVersion().equals(user.getTokenVersion())) {
            return;
        }
        var roles = userRoleMapper.selectRoleCodesByUserId(user.getId());
        var currentUser = new CurrentUser(user.getId(), user.getUsername(), user.getEmail(), roles, user.getTokenVersion());
        CurrentUserContext.set(currentUser);
        var authorities = roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .toList();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(currentUser, null, authorities)
        );
    }
}
