package com.labelhub.modules.review.port;

import org.springframework.stereotype.Component;

@Component
public class NoOpSubmissionEventPublisher implements SubmissionEventPublisher {

    @Override
    public void publishApproved(Long submissionId, Long reviewerId) {
        // BE-B 接入后替换为真实实现
    }
}
