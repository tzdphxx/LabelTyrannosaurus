package com.labelhub.infrastructure.notification;

import java.util.List;

public interface NotificationStore {

    void save(NotificationEvent event);

    List<NotificationEvent> findByUserId(Long userId, int limit, int offset);

    long countUnread(Long userId);

    void markRead(Long userId, String notificationId);

    void markAllRead(Long userId);
}
