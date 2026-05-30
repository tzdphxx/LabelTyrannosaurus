package com.labelhub.modules.agent.dto;

import com.labelhub.modules.agent.domain.AgentRunStatus;
import java.time.LocalDateTime;
import java.util.Map;

public record AgentRunDetailResponse(
        Long id,
        String agentType,
        Long submissionId,
        Long providerId,
        String modelName,
        String promptVersion,
        AgentRunStatus status,
        Map<String, Object> inputSnapshot,
        Map<String, Object> outputSnapshot,
        String errorMessage,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        LocalDateTime createdAt
) {}
