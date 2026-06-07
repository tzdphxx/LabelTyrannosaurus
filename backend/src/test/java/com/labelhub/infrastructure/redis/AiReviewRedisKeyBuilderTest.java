package com.labelhub.infrastructure.redis;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiReviewRedisKeyBuilderTest {

    @Test
    void buildsAiReviewStreamKey() {
        assertThat(RedisKeyBuilder.aiReviewStream(7L))
                .isEqualTo("ai:review:stream:task:7");
    }

    @Test
    void buildsCacheAndLockKeys() {
        assertThat(RedisKeyBuilder.taskClaimLock(9L))
                .isEqualTo("lock:claim:task:9");
        assertThat(RedisKeyBuilder.assignmentDraft(10L))
                .isEqualTo("cache:assignment:draft:10");
        assertThat(RedisKeyBuilder.dashboard("owner", 5L, "30d"))
                .isEqualTo("cache:dashboard:owner:5:30d");
        assertThat(RedisKeyBuilder.templateSchema(12L))
                .isEqualTo("cache:template:schema:12");
        assertThat(RedisKeyBuilder.taskTags(13L))
                .isEqualTo("cache:task:tags:13");
        assertThat(RedisKeyBuilder.userRoles(14L))
                .isEqualTo("cache:user:roles:14");
    }
}
