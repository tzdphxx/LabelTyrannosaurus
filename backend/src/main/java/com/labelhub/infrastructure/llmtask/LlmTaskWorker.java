package com.labelhub.infrastructure.llmtask;

import com.labelhub.infrastructure.redis.RedisLockService;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class LlmTaskWorker {

    private static final Logger log = LoggerFactory.getLogger(LlmTaskWorker.class);

    private final LlmTaskQueueService queueService;
    private final LlmTaskQueueProperties properties;
    private final RedisLockService redisLockService;
    private final Map<LlmTaskType, LlmTaskHandler> handlers;
    private final String consumerName = "worker-" + UUID.randomUUID();
    private final Map<LlmTaskType, String> claimStartIds = new EnumMap<>(LlmTaskType.class);

    public LlmTaskWorker(LlmTaskQueueService queueService,
                         LlmTaskQueueProperties properties,
                         RedisLockService redisLockService,
                         List<LlmTaskHandler> handlers) {
        this.queueService = queueService;
        this.properties = properties;
        this.redisLockService = redisLockService;
        this.handlers = new EnumMap<>(LlmTaskType.class);
        for (LlmTaskHandler handler : handlers) {
            this.handlers.put(handler.taskType(), handler);
        }
    }

    @Scheduled(fixedDelayString = "${labelhub.redis.llm-task-queue.poll-delay-ms:1000}")
    public void poll() {
        for (LlmTaskType taskType : LlmTaskType.values()) {
            processRecords(taskType, queueService.read(taskType, consumerName,
                    properties.defaultBatchSize(), properties.readWait()));
        }
    }

    @Scheduled(fixedDelayString = "${labelhub.redis.llm-task-queue.claim-delay-ms:30000}")
    public void claimStale() {
        for (LlmTaskType taskType : LlmTaskType.values()) {
            String startId = claimStartIds.get(taskType);
            LlmTaskClaimResult result = queueService.claimStale(taskType, consumerName,
                    properties.pendingMinIdle(), startId, properties.defaultBatchSize());
            claimStartIds.put(taskType, result.nextStartMessageId());
            processRecords(taskType, result.records());
        }
    }

    private void processRecords(LlmTaskType taskType, List<LlmTaskQueueRecord> records) {
        LlmTaskHandler handler = handlers.get(taskType);
        if (handler == null) {
            return;
        }
        for (LlmTaskQueueRecord record : records) {
            processRecord(handler, record);
        }
    }

    private void processRecord(LlmTaskHandler handler, LlmTaskQueueRecord record) {
        LlmTaskQueueMessage message = record.message();
        String lockKey = "lock:llm-task:%s:%s".formatted(message.taskType(), message.bizId());
        try {
            redisLockService.withLock(lockKey, 1000L, 300000L, () -> {
                if (handler.isCompleted(message)) {
                    queueService.ack(message.taskType(), record.messageId());
                    return;
                }
                handler.handle(message);
                queueService.ack(message.taskType(), record.messageId());
            });
        } catch (Exception ex) {
            log.warn("LLM task execution failed: type={}, bizId={}, messageId={}",
                    message.taskType(), message.bizId(), record.messageId(), ex);
        }
    }
}
