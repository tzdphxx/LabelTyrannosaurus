package com.labelhub.modules.review.port;

import org.springframework.stereotype.Component;

@Component
public class NoOpSubmissionEventPublisher implements SubmissionEventPublisher {

    @Override
    public void publishApproved(Long submissionId, Long reviewerId) {
    }

    @Override
    public void publishGoldenSelected(Long submissionId, Long reviewerId) {
    }
}
