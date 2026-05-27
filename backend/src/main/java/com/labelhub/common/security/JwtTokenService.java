package com.labelhub.common.security;

import com.labelhub.common.exception.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JWT 签发与解析服务。
 *
 * <p>accessToken 携带 userId、username、roles 和 tokenVersion；refreshToken
 * 只用于换取新令牌。所有解析失败统一转换为 {@code 401001}，避免泄露 token 细节。</p>
 */
@Service
public class JwtTokenService {

    private static final String TOKEN_VERSION = "tokenVersion";
    private static final String ROLES = "roles";
    private static final String REFRESH = "refresh";

    private final JwtProperties properties;

    public JwtTokenService(JwtProperties properties) {
        this.properties = properties;
    }

    /**
     * 创建访问令牌，用于普通业务接口鉴权。
     */
    public String createAccessToken(Long userId, String username, Set<RoleCode> roles, Integer tokenVersion) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim(ROLES, roles.stream().map(Enum::name).toList())
                .claim(TOKEN_VERSION, tokenVersion)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(properties.getAccessTokenTtlMinutes() * 60)))
                .signWith(secretKey())
                .compact();
    }

    /**
     * 创建刷新令牌，用于换取新的访问令牌。
     */
    public String createRefreshToken(Long userId, String username, Integer tokenVersion) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim(ROLES, List.of())
                .claim(TOKEN_VERSION, tokenVersion)
                .claim(REFRESH, true)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(properties.getRefreshTokenTtlDays() * 24 * 60 * 60)))
                .signWith(secretKey())
                .compact();
    }

    /**
     * 解析访问令牌，并拒绝误用 refreshToken 访问业务接口。
     */
    public TokenClaims parseAccessToken(String token) {
        TokenClaims claims = parse(token);
        if (claims.refresh()) {
            throw new BusinessException(401001, "Invalid access token");
        }
        return claims;
    }

    /**
     * 解析刷新令牌，并拒绝误用 accessToken 刷新令牌。
     */
    public TokenClaims parseRefreshToken(String token) {
        TokenClaims claims = parse(token);
        if (!claims.refresh()) {
            throw new BusinessException(401001, "Invalid refresh token");
        }
        return claims;
    }

    private TokenClaims parse(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(secretKey()).build().parseSignedClaims(token).getPayload();
            Long userId = Long.valueOf(claims.getSubject());
            String username = claims.get("username", String.class);
            Integer tokenVersion = claims.get(TOKEN_VERSION, Integer.class);
            Boolean refresh = claims.get(REFRESH, Boolean.class);
            @SuppressWarnings("unchecked")
            List<String> roleNames = claims.get(ROLES, List.class);
            Set<RoleCode> roles = roleNames.stream()
                    .map(String::valueOf)
                    .map(RoleCode::valueOf)
                    .collect(Collectors.toSet());
            return new TokenClaims(userId, username, roles, tokenVersion, Boolean.TRUE.equals(refresh));
        } catch (RuntimeException ex) {
            throw new BusinessException(401001, "Invalid token");
        }
    }

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public record TokenClaims(Long userId, String username, Set<RoleCode> roles, Integer tokenVersion, boolean refresh) {
    }
}
