package com.labelhub.infrastructure.llmtask;

import com.labelhub.infrastructure.redis.RedisKeyBuilder;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
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

@Service
public class RedissonLlmTaskQueueService implements LlmTaskQueueService {

    private static final String BUSY_GROUP_ERROR = "BUSYGROUP";
    private static final String DEFAULT_START_MESSAGE_ID = "0-0";
    private static final StreamMessageId GROUP_START_MESSAGE_ID = new StreamMessageId(0L, 0L);

    private final RedissonClient redissonClient;
    private final LlmTaskQueueProperties properties;

    public RedissonLlmTaskQueueService(RedissonClient redissonClient, LlmTaskQueueProperties properties) {
        this.redissonClient = redissonClient;
        this.properties = properties;
    }

    @Override
    public String enqueue(LlmTaskQueueMessage message) {
        RStream<String, String> stream = stream(message.taskType());
        StreamMessageId messageId = stream.add(StreamAddArgs.entries(toPayload(message)));
        return messageId.toString();
    }

    @Override
    public List<LlmTaskQueueRecord> read(LlmTaskType taskType, String consumerName, int count, Duration waitTime) {
        RStream<String, String> stream = stream(taskType);
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
    public LlmTaskClaimResult claimStale(
            LlmTaskType taskType,
            String consumerName,
            Duration minIdleTime,
            String startMessageId,
            int count) {
        RStream<String, String> stream = stream(taskType);
        ensureConsumerGroup(stream);
        AutoClaimResult<String, String> result = stream.autoClaim(
                properties.consumerGroup(),
                requireText(consumerName, "consumerName"),
                normalizeDuration(minIdleTime, properties.pendingMinIdle()).toMillis(),
                TimeUnit.MILLISECONDS,
                parseMessageId(StringUtils.hasText(startMessageId) ? startMessageId : DEFAULT_START_MESSAGE_ID),
                normalizeCount(count)
        );
        return new LlmTaskClaimResult(result.getNextId().toString(), toRecords(result.getMessages()));
    }

    @Override
    public boolean ack(LlmTaskType taskType, String messageId) {
        RStream<String, String> stream = stream(taskType);
        StreamMessageId streamMessageId = parseMessageId(messageId);
        long acked = stream.ack(properties.consumerGroup(), streamMessageId);
        stream.remove(streamMessageId);
        return acked > 0;
    }

    static Map<String, String> toPayload(LlmTaskQueueMessage message) {
        Objects.requireNonNull(message, "message must not be null");
        requireNonNull(message.taskType(), "taskType");
        requireNonNull(message.bizId(), "bizId");

        Map<String, String> payload = new LinkedHashMap<>();
        payload.put("taskType", message.taskType().name());
        payload.put("bizId", message.bizId().toString());
        payload.put("taskId", text(message.taskId()));
        payload.put("assignmentId", text(message.assignmentId()));
        payload.put("submissionId", text(message.submissionId()));
        payload.put("preAnnotationId", text(message.preAnnotationId()));
        payload.put("triggerRunId", text(message.triggerRunId()));
        payload.put("agentRunId", text(message.agentRunId()));
        payload.put("traceId", message.traceId() == null ? "" : message.traceId());
        payload.put("retryCount", String.valueOf(message.retryCount() == null ? 0 : message.retryCount()));
        payload.put("createdAt", (message.createdAt() == null ? Instant.now() : message.createdAt()).toString());
        return payload;
    }

    private RStream<String, String> stream(LlmTaskType taskType) {
        requireNonNull(taskType, "taskType");
        return redissonClient.getStream(RedisKeyBuilder.llmTaskStream(taskType.name()), StringCodec.INSTANCE);
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

    private static List<LlmTaskQueueRecord> toRecords(Map<StreamMessageId, Map<String, String>> messages) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        return messages.entrySet().stream()
                .map(entry -> new LlmTaskQueueRecord(entry.getKey().toString(), fromPayload(entry.getValue())))
                .toList();
    }

    private static LlmTaskQueueMessage fromPayload(Map<String, String> payload) {
        LlmTaskType taskType = LlmTaskType.valueOf(requireText(payload.get("taskType"), "taskType"));
        return new LlmTaskQueueMessage(
                taskType,
                parseLong(payload.get("bizId")),
                parseLong(payload.get("taskId")),
                parseLong(payload.get("assignmentId")),
                parseLong(payload.get("submissionId")),
                parseLong(payload.get("preAnnotationId")),
                parseLong(payload.get("triggerRunId")),
                parseLong(payload.get("agentRunId")),
                payload.getOrDefault("traceId", ""),
                parseInt(payload.get("retryCount"), 0),
                Instant.parse(requireText(payload.get("createdAt"), "createdAt"))
        );
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

    private static StreamMessageId parseMessageId(String messageId) {
        String value = requireText(messageId, "messageId");
        String[] parts = value.split("-", -1);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid Redis stream messageId: " + messageId);
        }
        return new StreamMessageId(Long.parseLong(parts[0]), Long.parseLong(parts[1]));
    }

    private static Long parseLong(String value) {
        return StringUtils.hasText(value) ? Long.valueOf(value) : null;
    }

    private static int parseInt(String value, int defaultValue) {
        return StringUtils.hasText(value) ? Integer.parseInt(value) : defaultValue;
    }

    private static String text(Long value) {
        return value == null ? "" : value.toString();
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
