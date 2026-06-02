package com.labelhub.infrastructure.notification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import org.springframework.stereotype.Component;

@Component
public class InMemoryNotificationStore implements NotificationStore {

    private static final int MAX_PER_USER = 100;

    private final ConcurrentHashMap<Long, ConcurrentLinkedDeque<NotificationEvent>> store =
            new ConcurrentHashMap<>();

    @Override
    public void save(NotificationEvent event) {
        ConcurrentLinkedDeque<NotificationEvent> deque =
                store.computeIfAbsent(event.userId(), k -> new ConcurrentLinkedDeque<>());
        deque.addFirst(event);
        while (deque.size() > MAX_PER_USER) {
            deque.removeLast();
        }
    }

    @Override
    public List<NotificationEvent> findByUserId(Long userId, int limit, int offset) {
        ConcurrentLinkedDeque<NotificationEvent> deque = store.get(userId);
        if (deque == null) return Collections.emptyList();
        List<NotificationEvent> all = new ArrayList<>(deque);
        int end = Math.min(offset + limit, all.size());
        if (offset >= all.size()) return Collections.emptyList();
        return all.subList(offset, end);
    }

    @Override
    public long countUnread(Long userId) {
        ConcurrentLinkedDeque<NotificationEvent> deque = store.get(userId);
        if (deque == null) return 0;
        return deque.stream().filter(e -> !e.read()).count();
    }

    @Override
    public void markRead(Long userId, String notificationId) {
        ConcurrentLinkedDeque<NotificationEvent> deque = store.get(userId);
        if (deque == null) return;
        ConcurrentLinkedDeque<NotificationEvent> updated = new ConcurrentLinkedDeque<>();
        for (NotificationEvent e : deque) {
            updated.add(e.id().equals(notificationId) ? e.withRead(true) : e);
        }
        store.put(userId, updated);
    }

    @Override
    public void markAllRead(Long userId) {
        ConcurrentLinkedDeque<NotificationEvent> deque = store.get(userId);
        if (deque == null) return;
        ConcurrentLinkedDeque<NotificationEvent> updated = new ConcurrentLinkedDeque<>();
        for (NotificationEvent e : deque) {
            updated.add(e.read() ? e : e.withRead(true));
        }
        store.put(userId, updated);
    }
}
