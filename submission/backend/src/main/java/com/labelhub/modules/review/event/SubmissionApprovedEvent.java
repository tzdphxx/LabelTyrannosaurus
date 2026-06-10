package com.labelhub.modules.review.event;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.context.ApplicationEvent;

public class SubmissionApprovedEvent extends ApplicationEvent {

    private final String eventId;
    private final Long taskId;
    private final Long datasetItemId;
    private final Long assignmentId;
    private final Long submissionId;
    private final Long labelerId;
    private final Long reviewerId;
    private final LocalDateTime approvedAt;

    public SubmissionApprovedEvent(Object source, Long taskId, Long datasetItemId,
                                    Long assignmentId, Long submissionId,
                                    Long labelerId, Long reviewerId,
                                    LocalDateTime approvedAt) {
        super(source);
        this.eventId = UUID.randomUUID().toString();
        this.taskId = taskId;
        this.datasetItemId = datasetItemId;
        this.assignmentId = assignmentId;
        this.submissionId = submissionId;
        this.labelerId = labelerId;
        this.reviewerId = reviewerId;
        this.approvedAt = approvedAt;
    }

    public String getEventId() { return eventId; }
    public Long getTaskId() { return taskId; }
    public Long getDatasetItemId() { return datasetItemId; }
    public Long getAssignmentId() { return assignmentId; }
    public Long getSubmissionId() { return submissionId; }
    public Long getLabelerId() { return labelerId; }
    public Long getReviewerId() { return reviewerId; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
}
