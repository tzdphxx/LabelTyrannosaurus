package com.labelhub.modules.submission.repository;

import java.time.LocalDateTime;

/**
 * 导出快照 Mapper 原始记录，JSON 字段在 Service 层统一解析。
 */
public record ExportableSubmissionRecord(Long submissionId,
                                         Long datasetItemId,
                                         String itemJson,
                                         String answerJson,
                                         String aiReviewJson,
                                         String reviewComment,
                                         Long labelerId,
                                         String username,
                                         String displayName,
                                         String email,
                                         LocalDateTime submittedAt) {
}
