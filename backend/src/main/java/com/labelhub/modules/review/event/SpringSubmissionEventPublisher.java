package com.labelhub.modules.review.event;

import com.labelhub.modules.review.port.SubmissionEventPublisher;
import com.labelhub.modules.submission.domain.Submission;
import com.labelhub.modules.submission.mapper.SubmissionMapper;
import java.time.LocalDateTime;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
public class SpringSubmissionEventPublisher implements SubmissionEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;
    private final SubmissionMapper submissionMapper;

    public SpringSubmissionEventPublisher(ApplicationEventPublisher applicationEventPublisher,
                                          SubmissionMapper submissionMapper) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.submissionMapper = submissionMapper;
    }

    @Override
    public void publishApproved(Long submissionId, Long reviewerId) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            return;
        }
        applicationEventPublisher.publishEvent(new SubmissionApprovedEvent(
                this,
                submission.getTaskId(),
                submission.getDatasetItemId(),
                submission.getAssignmentId(),
                submission.getId(),
                submission.getLabelerId(),
                reviewerId,
                LocalDateTime.now()
        ));
    }

    @Override
    public void publishGoldenSelected(Long submissionId, Long reviewerId) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            return;
        }
        applicationEventPublisher.publishEvent(new GoldenSelectedEvent(
                this,
                submission.getTaskId(),
                submission.getDatasetItemId(),
                null,
                submission.getId(),
                submission.getLabelerId(),
                reviewerId,
                LocalDateTime.now()
        ));
    }
}
