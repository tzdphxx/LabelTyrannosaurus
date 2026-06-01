package com.labelhub.modules.submission.dto;

import com.labelhub.modules.submission.domain.SubmissionStatus;
import java.time.LocalDateTime;

public record VersionHistoryItem(
        Long submissionId,
        Integer versionNo,
        SubmissionStatus status,
        String answerHash,
        Boolean isGolden,
        LocalDateTime submittedAt,
        String aiDecision,
        String aiFlowAction,
        String latestReviewAction
) {}
