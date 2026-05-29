package com.labelhub.infrastructure.ai;

import org.junit.jupiter.api.Test;
import org.redisson.api.AutoClaimResult;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.api.stream.StreamCreateGroupArgs;
import org.redisson.api.stream.StreamReadGroupArgs;
import org.redisson.client.RedisException;
import org.redisson.client.codec.StringCodec;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedissonAiReviewQueueServiceTest {

    private final RedissonClient redissonClient = mock(RedissonClient.class);
    private final RStream<String, String> stream = mock(RStream.class);
    private final AiReviewQueueProperties properties = new AiReviewQueueProperties(
            "ai-review-workers",
            10,
            Duration.ofSeconds(2),
            Duration.ofMinutes(5)
    );
    private final RedissonAiReviewQueueService queueService =
            new RedissonAiReviewQueueService(redissonClient, properties);

    @Test
    void enqueueWritesMessageToTaskStreamAndReturnsMessageId() {
        when(redissonClient.<String, String>getStream(eq("ai:review:stream:task:7"), eq(StringCodec.INSTANCE)))
                .thenReturn(stream);
        when(stream.add(any(StreamAddArgs.class))).thenReturn(new StreamMessageId(1710000000000L, 0L));

        String messageId = queueService.enqueue(new AiReviewQueueMessage(
                7L,
                101L,
                201L,
                301L,
                "trace-1",
                0,
                Instant.parse("2026-05-30T01:00:00Z")
        ));

        assertThat(messageId).isEqualTo("1710000000000-0");
        verify(stream).add(any(StreamAddArgs.class));
        assertThat(RedissonAiReviewQueueService.toPayload(new AiReviewQueueMessage(
                7L,
                101L,
                201L,
                301L,
                "trace-1",
                0,
                Instant.parse("2026-05-30T01:00:00Z")
        ))).containsExactlyInAnyOrderEntriesOf(Map.of(
                "taskId", "7",
                "submissionId", "101",
                "assignmentId", "201",
                "labelerId", "301",
                "traceId", "trace-1",
                "retryCount", "0",
                "createdAt", "2026-05-30T01:00:00Z"
        ));
    }

    @Test
    void readCreatesGroupAndReturnsNeverDeliveredRecords() {
        when(redissonClient.<String, String>getStream(eq("ai:review:stream:task:7"), eq(StringCodec.INSTANCE)))
                .thenReturn(stream);
        Map<StreamMessageId, Map<String, String>> messages = new LinkedHashMap<>();
        messages.put(new StreamMessageId(1710000000000L, 0L), payload());
        when(stream.readGroup(eq("ai-review-workers"), eq("worker-1"), any(StreamReadGroupArgs.class)))
                .thenReturn(messages);

        List<AiReviewQueueRecord> records = queueService.read(7L, "worker-1", 5, Duration.ofSeconds(1));

        assertThat(records).containsExactly(new AiReviewQueueRecord(
                "1710000000000-0",
                new AiReviewQueueMessage(
                        7L,
                        101L,
                        201L,
                        301L,
                        "trace-1",
                        0,
                        Instant.parse("2026-05-30T01:00:00Z")
                )
        ));
        verify(stream).createGroup(any(StreamCreateGroupArgs.class));
        verify(stream).readGroup(eq("ai-review-workers"), eq("worker-1"), any(StreamReadGroupArgs.class));
    }

    @Test
    void readIgnoresExistingConsumerGroupError() {
        when(redissonClient.<String, String>getStream(eq("ai:review:stream:task:7"), eq(StringCodec.INSTANCE)))
                .thenReturn(stream);
        doThrow(new RedisException("BUSYGROUP Consumer Group name already exists"))
                .when(stream)
                .createGroup(any(StreamCreateGroupArgs.class));
        when(stream.readGroup(eq("ai-review-workers"), eq("worker-1"), any(StreamReadGroupArgs.class)))
                .thenReturn(Map.of());

        List<AiReviewQueueRecord> records = queueService.read(7L, "worker-1", 5, Duration.ofSeconds(1));

        assertThat(records).isEmpty();
    }

    @Test
    void claimStaleAutoClaimsPendingRecordsAndReturnsNextStartMessageId() {
        when(redissonClient.<String, String>getStream(eq("ai:review:stream:task:7"), eq(StringCodec.INSTANCE)))
                .thenReturn(stream);
        Map<StreamMessageId, Map<String, String>> messages = new LinkedHashMap<>();
        messages.put(new StreamMessageId(1710000000000L, 0L), payload());
        when(stream.autoClaim(
                eq("ai-review-workers"),
                eq("worker-2"),
                eq(300_000L),
                eq(TimeUnit.MILLISECONDS),
                eq(new StreamMessageId(0L, 0L)),
                eq(10)
        )).thenReturn(new AutoClaimResult<>(new StreamMessageId(1710000000001L, 0L), messages, List.of()));

        AiReviewClaimResult result = queueService.claimStale(
                7L,
                "worker-2",
                Duration.ofMinutes(5),
                "0-0",
                10
        );

        assertThat(result.nextStartMessageId()).isEqualTo("1710000000001-0");
        assertThat(result.records()).hasSize(1);
        assertThat(result.records().get(0).message().submissionId()).isEqualTo(101L);
    }

    @Test
    void ackAcknowledgesAndRemovesConfirmedMessage() {
        when(redissonClient.<String, String>getStream(eq("ai:review:stream:task:7"), eq(StringCodec.INSTANCE)))
                .thenReturn(stream);
        StreamMessageId messageId = new StreamMessageId(1710000000000L, 0L);
        when(stream.ack("ai-review-workers", messageId)).thenReturn(1L);

        boolean acked = queueService.ack(7L, "1710000000000-0");

        assertThat(acked).isTrue();
        verify(stream).ack("ai-review-workers", messageId);
        verify(stream).remove(messageId);
    }

    private static Map<String, String> payload() {
        return Map.of(
                "taskId", "7",
                "submissionId", "101",
                "assignmentId", "201",
                "labelerId", "301",
                "traceId", "trace-1",
                "retryCount", "0",
                "createdAt", "2026-05-30T01:00:00Z"
        );
    }
}
