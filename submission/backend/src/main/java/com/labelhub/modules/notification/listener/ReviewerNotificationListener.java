package com.labelhub.modules.notification.listener;

import com.labelhub.infrastructure.notification.NotificationService;
import com.labelhub.modules.review.event.SubmissionAssignedEvent;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class ReviewerNotificationListener {

    private final NotificationService notificationService;

    public ReviewerNotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Async
    @EventListener
    public void onSubmissionAssigned(SubmissionAssignedEvent event) {
        notificationService.notify(
                event.getReviewerId(),
                "new_submission_assigned",
                "新提交待审核",
                "有新的提交 #" + event.getSubmissionId() + " 分配给您审核",
                Map.of("submissionId", event.getSubmissionId(),
                       "taskId", event.getTaskId()));
    }
}
