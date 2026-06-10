package com.labelhub.modules.review.port;

public class NoOpSubmissionEventPublisher implements SubmissionEventPublisher {

    @Override
    public void publishApproved(Long submissionId, Long reviewerId) {
    }

    @Override
    public void publishRejected(Long submissionId, Long reviewerId, String reason) {
    }

    @Override
    public void publishGoldenSelected(Long conflictGroupId, Long submissionId, Long reviewerId) {
    }
}
