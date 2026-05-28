package com.labelhub.modules.review.port;

public interface SubmissionEventPublisher {

    void publishApproved(Long submissionId, Long reviewerId);

    void publishGoldenSelected(Long submissionId, Long reviewerId);

    default void publishRewardReversed(Long submissionId, Long operatorId, String reason) {
    }
}
