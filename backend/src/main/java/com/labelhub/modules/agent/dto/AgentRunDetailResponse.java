package com.labelhub.modules.agent.dto;

import com.labelhub.modules.agent.domain.AgentRunStatus;
import java.time.LocalDateTime;
import java.util.Map;

public record AgentRunDetailResponse(
        Long agentRunId,
        String agentType,
        Long submissionId,
        Long assignmentId,
        Long providerId,
        String modelName,
        String promptVersion,
        AgentRunStatus status,
        Map<String, Object> inputSnapshot,
        Map<String, Object> outputSnapshot,
        String errorMessage,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        LocalDateTime createdAt,
        boolean redacted
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
