package com.labelhub.modules.submission.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * BE-A 提供给 BE-B 的导出快照。
 *
 * <p>该快照只读，BE-B 不得通过它回写 submission、review 或 golden 状态。</p>
 */
public record ExportableSubmissionSnapshot(Long submissionId,
                                           Long datasetItemId,
                                           JsonNode itemSnapshot,
                                           JsonNode answerJson,
                                           JsonNode aiReviewSnapshot,
                                           List<AuditRef> auditRefs,
                                           String reviewComment,
                                           LabelerInfo labelerInfo,
                                           LocalDateTime submittedAt) {
}
