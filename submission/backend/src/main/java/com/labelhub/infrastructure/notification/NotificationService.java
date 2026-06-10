package com.labelhub.infrastructure.notification;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private final NotificationStore store;
    private final SseConnectionManager sseConnectionManager;

    public NotificationService(NotificationStore store,
                               SseConnectionManager sseConnectionManager) {
        this.store = store;
        this.sseConnectionManager = sseConnectionManager;
    }

    public void notify(Long userId, String type, String title,
                       String body, Map<String, Object> data) {
        NotificationEvent event = new NotificationEvent(
                UUID.randomUUID().toString(),
                type, userId, title, body, data,
                LocalDateTime.now(), false);
        store.save(event);
        sseConnectionManager.send(userId, event);
    }

    public void broadcast(String type, String title, String body,
                          Map<String, Object> data) {
        // For system announcements — not user-targeted
        // In a real impl this would iterate all connected users
    }
}
