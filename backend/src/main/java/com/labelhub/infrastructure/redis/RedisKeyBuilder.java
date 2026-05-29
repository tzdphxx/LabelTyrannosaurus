package com.labelhub.infrastructure.redis;

public final class RedisKeyBuilder {

    private RedisKeyBuilder() {
    }

    public static String claimLock(Long taskId, Long itemId) {
        return "lock:claim:task:%d:item:%d".formatted(taskId, itemId);
    }

    public static String assignmentDraft(Long assignmentId) {
        return "draft:assignment:%d".formatted(assignmentId);
    }

    public static String llmRate(String scope, Object id) {
        return "llm:rate:%s:%s".formatted(scope, id);
    }

    public static String eventDedup(String eventType, Object eventId) {
        return "event:dedup:%s:%s".formatted(eventType, eventId);
    }

    public static String rewardRule(Long taskId) {
        return "lock:reward-rule:task:%d".formatted(taskId);
    }
}
