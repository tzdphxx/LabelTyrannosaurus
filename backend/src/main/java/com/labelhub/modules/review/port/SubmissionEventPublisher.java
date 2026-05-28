package com.labelhub.modules.review.port;

public interface SubmissionEventPublisher {

    void publishApproved(Long submissionId, Long reviewerId);
}
