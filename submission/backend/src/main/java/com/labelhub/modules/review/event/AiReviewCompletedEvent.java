package com.labelhub.modules.review.event;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.context.ApplicationEvent;

public class AiReviewCompletedEvent extends ApplicationEvent {

    private final String eventId;
    private final Long taskId;
    private final Long submissionId;
    private final Long labelerId;
    private final String decision;
    private final LocalDateTime completedAt;

    public AiReviewCompletedEvent(Object source, Long taskId,
                                  Long submissionId, Long labelerId,
                                  String decision, LocalDateTime completedAt) {
        super(source);
        this.eventId = UUID.randomUUID().toString();
        this.taskId = taskId;
        this.submissionId = submissionId;
        this.labelerId = labelerId;
        this.decision = decision;
        this.completedAt = completedAt;
    }

    public String getEventId() { return eventId; }
    public Long getTaskId() { return taskId; }
    public Long getSubmissionId() { return submissionId; }
    public Long getLabelerId() { return labelerId; }
    public String getDecision() { return decision; }
    public LocalDateTime getCompletedAt() { return completedAt; }
}
