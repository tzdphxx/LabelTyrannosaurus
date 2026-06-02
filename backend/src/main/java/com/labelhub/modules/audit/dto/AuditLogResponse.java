package com.labelhub.modules.audit.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "审计日志响应")
public record AuditLogResponse(
        @Schema(description = "审计日志ID") Long auditLogId,
        @Schema(description = "业务类型") String bizType,
        @Schema(description = "业务ID") Long bizId,
        @Schema(description = "操作者类型") String actorType,
        @Schema(description = "操作者ID") Long actorId,
        @Schema(description = "操作动作") String action,
        @Schema(description = "操作前JSON") JsonNode beforeJson,
        @Schema(description = "操作后JSON") JsonNode afterJson,
        @Schema(description = "追踪ID") String traceId,
        @Schema(description = "Agent运行ID") Long agentRunId,
        @Schema(description = "创建时间") LocalDateTime createdAt) {
}
