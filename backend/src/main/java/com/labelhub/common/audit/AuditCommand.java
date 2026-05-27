package com.labelhub.common.audit;

import java.util.Map;

/**
 * Unified audit append command shared by BE-A and BE-B.
 *
 * <p>Audit records are append-only. Callers provide immutable before/after
 * snapshots for traceability instead of relying on mutable business tables.</p>
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
