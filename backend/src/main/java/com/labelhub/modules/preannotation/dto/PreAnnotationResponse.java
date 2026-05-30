package com.labelhub.modules.preannotation.dto;

import com.labelhub.modules.preannotation.domain.PreAnnotationStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record PreAnnotationResponse(
        Long preAnnotationId,
        Long assignmentId,
        Long agentRunId,
        PreAnnotationStatus status,
        Map<String, Object> suggestedAnswerJson,
        List<Map<String, Object>> fieldSuggestions,
        List<String> riskFlags,
        BigDecimal overallConfidence,
        List<String> limitations,
        String promptMode,
        Boolean degraded,
        Map<String, Object> finalDiff,
        String errorCode,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
