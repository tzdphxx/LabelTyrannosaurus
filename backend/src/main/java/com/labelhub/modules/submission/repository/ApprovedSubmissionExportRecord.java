package com.labelhub.modules.submission.repository;

import java.time.LocalDateTime;

public record ApprovedSubmissionExportRecord(Long taskId,
                                             Long submissionId,
                                             Long datasetItemId,
                                             Long labelerId,
                                             Integer versionNo,
                                             LocalDateTime submittedAt,
                                             String itemJson,
                                             String answerJson,
                                             String aiReviewJson,
                                             String reviewComment) {
}
