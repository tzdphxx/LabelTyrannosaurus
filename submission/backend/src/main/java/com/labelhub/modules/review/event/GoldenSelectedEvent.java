package com.labelhub.modules.review.event;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.context.ApplicationEvent;

public class GoldenSelectedEvent extends ApplicationEvent {

    private final String eventId;
    private final Long taskId;
    private final Long datasetItemId;
    private final Long conflictGroupId;
    private final Long goldenSubmissionId;
    private final Long labelerId;
    private final Long reviewerId;
    private final LocalDateTime resolvedAt;

    public GoldenSelectedEvent(Object source, Long taskId, Long datasetItemId,
                                Long conflictGroupId, Long goldenSubmissionId,
                                Long labelerId, Long reviewerId,
                                LocalDateTime resolvedAt) {
        super(source);
        this.eventId = UUID.randomUUID().toString();
        this.taskId = taskId;
        this.datasetItemId = datasetItemId;
        this.conflictGroupId = conflictGroupId;
        this.goldenSubmissionId = goldenSubmissionId;
        this.labelerId = labelerId;
        this.reviewerId = reviewerId;
        this.resolvedAt = resolvedAt;
    }

    public String getEventId() { return eventId; }
    public Long getTaskId() { return taskId; }
    public Long getDatasetItemId() { return datasetItemId; }
    public Long getConflictGroupId() { return conflictGroupId; }
    public Long getGoldenSubmissionId() { return goldenSubmissionId; }
    public Long getLabelerId() { return labelerId; }
    public Long getReviewerId() { return reviewerId; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
}
