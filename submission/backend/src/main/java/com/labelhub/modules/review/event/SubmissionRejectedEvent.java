package com.labelhub.modules.review.event;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.context.ApplicationEvent;

public class SubmissionRejectedEvent extends ApplicationEvent {

    private final String eventId;
    private final Long taskId;
    private final Long assignmentId;
    private final Long submissionId;
    private final Long labelerId;
    private final Long reviewerId;
    private final String reason;
    private final LocalDateTime rejectedAt;

    public SubmissionRejectedEvent(Object source, Long taskId, Long assignmentId,
                                   Long submissionId, Long labelerId,
                                   Long reviewerId, String reason,
                                   LocalDateTime rejectedAt) {
        super(source);
        this.eventId = UUID.randomUUID().toString();
        this.taskId = taskId;
        this.assignmentId = assignmentId;
        this.submissionId = submissionId;
        this.labelerId = labelerId;
        this.reviewerId = reviewerId;
        this.reason = reason;
        this.rejectedAt = rejectedAt;
    }

    public String getEventId() { return eventId; }
    public Long getTaskId() { return taskId; }
    public Long getAssignmentId() { return assignmentId; }
    public Long getSubmissionId() { return submissionId; }
    public Long getLabelerId() { return labelerId; }
    public Long getReviewerId() { return reviewerId; }
    public String getReason() { return reason; }
    public LocalDateTime getRejectedAt() { return rejectedAt; }
}
