package com.labelhub.modules.notification.listener;

import com.labelhub.infrastructure.notification.NotificationService;
import com.labelhub.modules.review.event.AiReviewCompletedEvent;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class AiReviewNotificationListener {

    private final NotificationService notificationService;

    public AiReviewNotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @Async
    @EventListener
    public void onAiReviewCompleted(AiReviewCompletedEvent event) {
        notificationService.notify(
                event.getLabelerId(),
                "ai_review_completed",
                "AI 审核完成",
                "您的提交 #" + event.getSubmissionId() + " AI 审核已完成，结论：" + event.getDecision(),
                Map.of("submissionId", event.getSubmissionId(),
                       "taskId", event.getTaskId(),
                       "decision", event.getDecision()));
    }
}
