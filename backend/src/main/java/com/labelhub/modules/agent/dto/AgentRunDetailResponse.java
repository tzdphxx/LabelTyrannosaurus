package com.labelhub.modules.agent.dto;

import com.labelhub.modules.agent.domain.AgentRunStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "Agent运行详情响应")
public record AgentRunDetailResponse(
        @Schema(description = "Agent运行ID") Long agentRunId,
        @Schema(description = "Agent类型") String agentType,
        @Schema(description = "提交ID") Long submissionId,
        @Schema(description = "分配ID") Long assignmentId,
        @Schema(description = "供应商ID") Long providerId,
        @Schema(description = "模型名称") String modelName,
        @Schema(description = "提示版本") String promptVersion,
        @Schema(description = "运行状态") AgentRunStatus status,
        @Schema(description = "输入快照") Map<String, Object> inputSnapshot,
        @Schema(description = "输出快照") Map<String, Object> outputSnapshot,
        @Schema(description = "错误信息") String errorMessage,
        @Schema(description = "开始时间") LocalDateTime startedAt,
        @Schema(description = "完成时间") LocalDateTime finishedAt,
        @Schema(description = "创建时间") LocalDateTime createdAt,
        @Schema(description = "是否已脱敏") boolean redacted
) {
    public AgentRunDetailResponse(Long agentRunId, String agentType, Long submissionId,
                                  Long providerId, String modelName, String promptVersion,
                                  AgentRunStatus status, Map<String, Object> inputSnapshot,
                                  Map<String, Object> outputSnapshot, String errorMessage,
                                  LocalDateTime startedAt, LocalDateTime finishedAt,
                                  LocalDateTime createdAt, boolean redacted) {
        this(agentRunId, agentType, submissionId, null, providerId, modelName, promptVersion,
                status, inputSnapshot, outputSnapshot, errorMessage, startedAt, finishedAt, createdAt, redacted);
    }
}
