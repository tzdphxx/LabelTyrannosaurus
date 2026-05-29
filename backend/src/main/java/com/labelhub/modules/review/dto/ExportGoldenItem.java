package com.labelhub.modules.review.dto;

import com.labelhub.modules.ai.domain.AiDecision;

public record ExportGoldenItem(
        Long submissionId,
        Long datasetItemId,
        Long labelerId,
        Integer versionNo,
        String answerJson,
        AiDecision aiDecision,
        String aiSummary,
        Long auditLogId
) {
}
