package com.labelhub.modules.review.event;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.context.ApplicationEvent;

public class TaskStatusChangedEvent extends ApplicationEvent {

    private final String eventId;
    private final Long taskId;
    private final String taskTitle;
    private final String oldStatus;
    private final String newStatus;
    private final LocalDateTime changedAt;

    public TaskStatusChangedEvent(Object source, Long taskId, String taskTitle,
                                  String oldStatus, String newStatus,
                                  LocalDateTime changedAt) {
        super(source);
        this.eventId = UUID.randomUUID().toString();
        this.taskId = taskId;
        this.taskTitle = taskTitle;
        this.oldStatus = oldStatus;
        this.newStatus = newStatus;
        this.changedAt = changedAt;
    }

    public String getEventId() { return eventId; }
    public Long getTaskId() { return taskId; }
    public String getTaskTitle() { return taskTitle; }
    public String getOldStatus() { return oldStatus; }
    public String getNewStatus() { return newStatus; }
    public LocalDateTime getChangedAt() { return changedAt; }
}
