package com.labelhub.common.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.labelhub.modules.auth.domain.UserEntity;
import com.labelhub.modules.auth.repository.UserMapper;
import com.labelhub.modules.auth.repository.UserRoleMapper;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;

@Service
public class AuthUserCacheService {

    private static final Duration TTL = Duration.ofMinutes(10);

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;
    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;

    public AuthUserCacheService(RedissonClient redissonClient,
                                ObjectMapper objectMapper,
                                UserMapper userMapper,
                                UserRoleMapper userRoleMapper) {
        this.redissonClient = redissonClient;
        this.objectMapper = objectMapper;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
    }

    public Optional<CurrentUser> authenticate(JwtTokenService.TokenClaims claims) {
        return findCached(claims.userId())
                .or(() -> loadAndCache(claims.userId()))
                .filter(snapshot -> Boolean.TRUE.equals(snapshot.enabled()))
                .filter(snapshot -> claims.tokenVersion().equals(snapshot.tokenVersion()))
                .map(AuthUserSnapshot::toCurrentUser);
    }

    public void evict(Long userId) {
        if (userId == null) {
            return;
        }
        try {
            redissonClient.getBucket(key(userId)).delete();
        } catch (RuntimeException ignored) {
            // Redis cache eviction must not fail the database operation.
        }
    }

    private Optional<AuthUserSnapshot> findCached(Long userId) {
        try {
            RBucket<String> bucket = redissonClient.getBucket(key(userId));
            String json = bucket.get();
            if (json == null || json.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, AuthUserSnapshot.class));
        } catch (RuntimeException | JsonProcessingException ex) {
            return Optional.empty();
        }
    }

    private Optional<AuthUserSnapshot> loadAndCache(Long userId) {
        UserEntity user = userMapper.selectById(userId);
        if (user == null) {
            return Optional.empty();
        }
        Set<RoleCode> roles = userRoleMapper.selectRoleCodesByUserId(user.getId());
        AuthUserSnapshot snapshot = new AuthUserSnapshot(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getEnabled(),
                user.getTokenVersion(),
                roles == null ? Set.of() : roles);
        try {
            redissonClient.<String>getBucket(key(userId)).set(objectMapper.writeValueAsString(snapshot), TTL);
        } catch (RuntimeException | JsonProcessingException ignored) {
            // Authentication can continue with the database-backed snapshot.
        }
        return Optional.of(snapshot);
    }

    private String key(Long userId) {
        return "cache:auth:user:%d".formatted(userId);
    }

    private record AuthUserSnapshot(
            Long userId,
            String username,
            String email,
            Boolean enabled,
            Integer tokenVersion,
            Set<RoleCode> roles
    ) {

        private CurrentUser toCurrentUser() {
            return new CurrentUser(userId, username, email, roles, tokenVersion);
        }
    }
}
