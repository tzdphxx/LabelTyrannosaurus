package com.labelhub.modules.assignment.service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

@Service
public class RedissonAssignmentDraftCacheService implements AssignmentDraftCacheService {

    private static final long DRAFT_CACHE_TTL_HOURS = 24L;

    private final RedissonClient redissonClient;

    public RedissonAssignmentDraftCacheService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public Optional<AssignmentDraftCacheEntry> get(Long assignmentId) {
        return Optional.ofNullable(bucket(assignmentId).get());
    }

    @Override
    public void put(AssignmentDraftCacheEntry entry) {
        bucket(entry.assignmentId()).set(entry, DRAFT_CACHE_TTL_HOURS, TimeUnit.HOURS);
    }

    private RBucket<AssignmentDraftCacheEntry> bucket(Long assignmentId) {
        return redissonClient.getBucket("cache:assignment:draft:" + assignmentId);
    }
}
