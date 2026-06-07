package com.labelhub.infrastructure.redis;

public final class RedisKeyBuilder {

    private RedisKeyBuilder() {
    }

    public static String claimLock(Long taskId, Long itemId) {
        return "lock:claim:task:%d:item:%d".formatted(taskId, itemId);
    }

    public static String taskClaimLock(Long taskId) {
        return "lock:claim:task:%d".formatted(taskId);
    }

    public static String dashboard(String role, Object identity, String range) {
        return "cache:dashboard:%s:%s:%s".formatted(role, identity, range);
    }

    public static String templateSchema(Long templateVersionId) {
        return "cache:template:schema:%d".formatted(templateVersionId);
    }

    public static String taskTags(Long taskId) {
        return "cache:task:tags:%d".formatted(taskId);
    }

    public static String userRoles(Long userId) {
        return "cache:user:roles:%d".formatted(userId);
    }

    public static String assignmentDraft(Long assignmentId) {
        return "cache:assignment:draft:%d".formatted(assignmentId);
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

    public static String aiReviewStream(Long taskId) {
        return "ai:review:stream:task:%d".formatted(taskId);
    }

    public static String llmTaskStream(String taskType) {
        return "labelhub:llm:stream:%s".formatted(taskType.toLowerCase().replace('_', '-'));
    }

    public static String aiReviewLock(Long submissionId) {
        return "lock:ai-review:submission:%d".formatted(submissionId);
    }
}
