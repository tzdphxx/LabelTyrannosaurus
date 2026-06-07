package com.labelhub.modules.review.event;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.context.ApplicationEvent;

public class SubmissionAssignedEvent extends ApplicationEvent {

    private final String eventId;
    private final Long taskId;
    private final Long submissionId;
    private final Long reviewerId;
    private final LocalDateTime assignedAt;

    public SubmissionAssignedEvent(Object source, Long taskId,
                                   Long submissionId, Long reviewerId,
                                   LocalDateTime assignedAt) {
        super(source);
        this.eventId = UUID.randomUUID().toString();
        this.taskId = taskId;
        this.submissionId = submissionId;
        this.reviewerId = reviewerId;
        this.assignedAt = assignedAt;
    }

    public String getEventId() { return eventId; }
    public Long getTaskId() { return taskId; }
    public Long getSubmissionId() { return submissionId; }
    public Long getReviewerId() { return reviewerId; }
    public LocalDateTime getAssignedAt() { return assignedAt; }
}
