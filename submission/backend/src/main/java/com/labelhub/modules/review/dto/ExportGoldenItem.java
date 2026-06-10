package com.labelhub.modules.review.dto;

import com.labelhub.modules.ai.domain.AiDecision;

public record ExportGoldenItem(
        Long submissionId,
        Long taskId,
        Long datasetItemId,
        String itemJsonRef,
        Long labelerId,
        Integer versionNo,
        String answerJson,
        AiDecision aiDecision,
        String aiSummary,
        String reviewSummary,
        Long auditRef
) {
}
