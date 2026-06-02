package com.labelhub.modules.notification.listener;

import com.labelhub.infrastructure.notification.NotificationService;
import com.labelhub.modules.review.event.SubmissionApprovedEvent;
import com.labelhub.modules.review.event.SubmissionRejectedEvent;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class SubmissionNotificationListener {

    private final NotificationService notificationService;

    public SubmissionNotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Async
    @EventListener
    public void onApproved(SubmissionApprovedEvent event) {
        notificationService.notify(
                event.getLabelerId(),
                "submission_approved",
                "提交已通过",
                "您的提交 #" + event.getSubmissionId() + " 已通过审核",
                Map.of("submissionId", event.getSubmissionId(),
                       "taskId", event.getTaskId()));
    }

    @Async
    @EventListener
    public void onRejected(SubmissionRejectedEvent event) {
        notificationService.notify(
                event.getLabelerId(),
                "submission_rejected",
                "提交被驳回",
                "您的提交 #" + event.getSubmissionId() + " 被驳回，请修改后重新提交",
                Map.of("submissionId", event.getSubmissionId(),
                       "taskId", event.getTaskId(),
                       "assignmentId", event.getAssignmentId()));
    }
}
