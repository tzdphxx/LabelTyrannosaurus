package com.labelhub.infrastructure.redis;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiReviewRedisKeyBuilderTest {

    @Test
    void buildsAiReviewStreamKey() {
        assertThat(RedisKeyBuilder.aiReviewStream(7L))
                .isEqualTo("ai:review:stream:task:7");
    }
}
