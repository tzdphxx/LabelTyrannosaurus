package com.labelhub.modules.audit.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public record AuditLogResponse(Long auditLogId,
                               String bizType,
                               Long bizId,
                               String actorType,
                               Long actorId,
                               String action,
                               JsonNode beforeJson,
                               JsonNode afterJson,
                               String traceId,
                               Long agentRunId,
                               LocalDateTime createdAt) {
}
