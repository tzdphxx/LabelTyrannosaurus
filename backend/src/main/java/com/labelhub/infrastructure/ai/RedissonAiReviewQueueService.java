package com.labelhub.infrastructure.ai;

import com.labelhub.infrastructure.redis.RedisKeyBuilder;
import org.redisson.api.AutoClaimResult;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.api.stream.StreamCreateGroupArgs;
import org.redisson.api.stream.StreamReadGroupArgs;
import org.redisson.client.RedisException;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
public class RedissonAiReviewQueueService implements AiReviewQueueService {

    private static final String BUSY_GROUP_ERROR = "BUSYGROUP";
    private static final String DEFAULT_START_MESSAGE_ID = "0-0";
    private static final StreamMessageId GROUP_START_MESSAGE_ID = new StreamMessageId(0L, 0L);

    private final RedissonClient redissonClient;
    private final AiReviewQueueProperties properties;

    public RedissonAiReviewQueueService(RedissonClient redissonClient, AiReviewQueueProperties properties) {
        this.redissonClient = redissonClient;
        this.properties = properties;
    }

    @Override
    public String enqueue(AiReviewQueueMessage message) {
        RStream<String, String> stream = stream(message.taskId());
        StreamMessageId messageId = stream.add(StreamAddArgs.entries(toPayload(message)));
        return messageId.toString();
    }

    @Override
    public List<AiReviewQueueRecord> read(Long taskId, String consumerName, int count, Duration waitTime) {
        RStream<String, String> stream = stream(taskId);
        ensureConsumerGroup(stream);
        Map<StreamMessageId, Map<String, String>> messages = stream.readGroup(
                properties.consumerGroup(),
                requireText(consumerName, "consumerName"),
                StreamReadGroupArgs.neverDelivered()
                        .count(normalizeCount(count))
                        .timeout(normalizeDuration(waitTime, properties.readWait()))
        );
        return toRecords(messages);
    }

    @Override
    public AiReviewClaimResult claimStale(
            Long taskId,
            String consumerName,
            Duration minIdleTime,
            String startMessageId,
            int count
    ) {
        RStream<String, String> stream = stream(taskId);
        ensureConsumerGroup(stream);
        AutoClaimResult<String, String> result = stream.autoClaim(
                properties.consumerGroup(),
                requireText(consumerName, "consumerName"),
                normalizeDuration(minIdleTime, properties.pendingMinIdle()).toMillis(),
                TimeUnit.MILLISECONDS,
                parseMessageId(StringUtils.hasText(startMessageId) ? startMessageId : DEFAULT_START_MESSAGE_ID),
                normalizeCount(count)
        );
        return new AiReviewClaimResult(result.getNextId().toString(), toRecords(result.getMessages()));
    }

    @Override
    public boolean ack(Long taskId, String messageId) {
        RStream<String, String> stream = stream(taskId);
        StreamMessageId streamMessageId = parseMessageId(messageId);
        long acked = stream.ack(properties.consumerGroup(), streamMessageId);
        stream.remove(streamMessageId);
        return acked > 0;
    }

    static Map<String, String> toPayload(AiReviewQueueMessage message) {
        Objects.requireNonNull(message, "message must not be null");
        requireNonNull(message.taskId(), "taskId");
        requireNonNull(message.submissionId(), "submissionId");
        requireNonNull(message.assignmentId(), "assignmentId");
        requireNonNull(message.labelerId(), "labelerId");

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("taskId", message.taskId().toString());
        payload.put("submissionId", message.submissionId().toString());
        payload.put("assignmentId", message.assignmentId().toString());
        payload.put("labelerId", message.labelerId().toString());
        payload.put("traceId", message.traceId() == null ? "" : message.traceId());
        payload.put("retryCount", String.valueOf(message.retryCount() == null ? 0 : message.retryCount()));
        payload.put("createdAt", (message.createdAt() == null ? Instant.now() : message.createdAt()).toString());
        return payload;
    }

    private RStream<String, String> stream(Long taskId) {
        requireNonNull(taskId, "taskId");
        return redissonClient.getStream(RedisKeyBuilder.aiReviewStream(taskId), StringCodec.INSTANCE);
    }

    private void ensureConsumerGroup(RStream<String, String> stream) {
        try {
            stream.createGroup(StreamCreateGroupArgs.name(properties.consumerGroup())
                    .id(GROUP_START_MESSAGE_ID)
                    .makeStream());
        } catch (RedisException ex) {
            if (ex.getMessage() == null || !ex.getMessage().contains(BUSY_GROUP_ERROR)) {
                throw ex;
            }
        }
    }

    private static List<AiReviewQueueRecord> toRecords(Map<StreamMessageId, Map<String, String>> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        return messages.entrySet()
                .stream()
                .map(entry -> new AiReviewQueueRecord(entry.getKey().toString(), fromPayload(entry.getValue())))
                .toList();
    }

    private static AiReviewQueueMessage fromPayload(Map<String, String> payload) {
        return new AiReviewQueueMessage(
                parseLong(payload, "taskId"),
                parseLong(payload, "submissionId"),
                parseLong(payload, "assignmentId"),
                parseLong(payload, "labelerId"),
                payload.getOrDefault("traceId", ""),
                parseInt(payload.get("retryCount"), 0),
                Instant.parse(requireText(payload.get("createdAt"), "createdAt"))
        );
    }

    private static Long parseLong(Map<String, String> payload, String key) {
        return Long.valueOf(requireText(payload.get(key), key));
    }

    private static int parseInt(String value, int defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    private static StreamMessageId parseMessageId(String messageId) {
        String value = requireText(messageId, "messageId");
        String[] parts = value.split("-", -1);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid Redis stream messageId: " + messageId);
        }
        return new StreamMessageId(Long.parseLong(parts[0]), Long.parseLong(parts[1]));
    }

    private int normalizeCount(int count) {
        return count > 0 ? count : properties.defaultBatchSize();
    }

    private static Duration normalizeDuration(Duration duration, Duration defaultValue) {
        if (duration == null || duration.isNegative() || duration.isZero()) {
            return defaultValue;
        }
        return duration;
    }

    private static String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static void requireNonNull(Object value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + " must not be null");
        }
    }
}
