package com.labelhub.common.audit;

import java.util.Map;

public interface AuditAppender {

    Long append(String bizType,
                Long bizId,
                String actorType,
                Long actorId,
                String action,
                Map<String, Object> beforeJson,
                Map<String, Object> afterJson,
                String traceId,
                Long agentRunId);
}
