package com.labelhub.modules.submission.dto;

import com.labelhub.modules.submission.domain.SubmissionStatus;

public record SubmissionSubmitResponse(Long submissionId,
                                       Long assignmentId,
                                       Integer versionNo,
                                       SubmissionStatus status,
                                       String answerHash,
                                       Long agentRunId) {
}
