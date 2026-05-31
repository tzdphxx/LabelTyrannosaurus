package com.labelhub.modules.ai.dto;

import com.labelhub.modules.ai.domain.AiReviewStatus;
import java.util.Map;

public record AiReviewResultResponse(Long id,
                                     Long submissionId,
                                     Long agentRunId,
                                     Long providerId,
                                     String modelName,
                                     AiReviewStatus status,
                                     String decision,
                                     String averageScore,
                                     Map<String, Object> dimensionScores,
                                     String riskFlags,
                                     String suggestion,
                                     String errorCode,
                                     String errorMessage) {
}
