package com.labelhub.infrastructure.notification;

import java.time.LocalDateTime;
import java.util.Map;

public record NotificationEvent(
        String id,
        String type,
        Long userId,
        String title,
        String body,
        Map<String, Object> data,
        LocalDateTime createdAt,
        boolean read
) {
    public NotificationEvent withRead(boolean read) {
        return new NotificationEvent(id, type, userId, title, body, data, createdAt, read);
    }
}
