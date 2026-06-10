package com.labelhub.modules.notification.listener;

import com.labelhub.infrastructure.notification.NotificationService;
import com.labelhub.modules.assignment.mapper.AssignmentMapper;
import com.labelhub.modules.review.event.TaskStatusChangedEvent;
import java.util.List;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class TaskNotificationListener {

    private final NotificationService notificationService;
    private final AssignmentMapper assignmentMapper;

    public TaskNotificationListener(NotificationService notificationService,
                                    AssignmentMapper assignmentMapper) {
        this.notificationService = notificationService;
        this.assignmentMapper = assignmentMapper;
    }

    @Async
    @EventListener
    public void onTaskStatusChanged(TaskStatusChangedEvent event) {
        List<Long> labelerIds = assignmentMapper.selectDistinctLabelersByTask(event.getTaskId());
        for (Long labelerId : labelerIds) {
            notificationService.notify(
                    labelerId,
                    "task_status_changed",
                    "任务状态变更",
                    "任务「" + event.getTaskTitle() + "」已" + statusLabel(event.getNewStatus()),
                    Map.of("taskId", event.getTaskId(),
                           "oldStatus", event.getOldStatus(),
                           "newStatus", event.getNewStatus()));
        }
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "PAUSED" -> "暂停";
            case "PUBLISHED" -> "恢复";
            case "ENDED" -> "结束";
            default -> status;
        };
    }
}
