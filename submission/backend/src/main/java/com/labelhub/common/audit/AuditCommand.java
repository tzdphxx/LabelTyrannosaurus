package com.labelhub.common.audit;

import java.util.Map;

/**
 * BE-A 与 BE-B 共享的统一审计追加命令。
 *
 * <p>审计记录只允许追加。调用方需要传入操作前后快照，避免后续追溯依赖可变业务表。</p>
 */
public record AuditCommand(String actorType,
                           Long actorId,
                           String bizType,
                           Long bizId,
                           String action,
                           Map<String, Object> beforeJson,
                           Map<String, Object> afterJson,
                           String traceId,
                           Long agentRunId) {
}
