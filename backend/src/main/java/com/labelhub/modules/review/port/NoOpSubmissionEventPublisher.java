package com.labelhub.modules.review.port;

public class NoOpSubmissionEventPublisher implements SubmissionEventPublisher {

    @Override
    public void publishApproved(Long submissionId, Long reviewerId) {
    }

    @Override
    public void publishGoldenSelected(Long submissionId, Long reviewerId) {
    }
}
