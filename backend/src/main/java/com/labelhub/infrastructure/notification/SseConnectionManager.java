package com.labelhub.infrastructure.notification;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Component
public class SseConnectionManager {

    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<SseEmitter>> connections =
            new ConcurrentHashMap<>();

    public SseEmitter createConnection(Long userId, long timeout) {
        SseEmitter emitter = new SseEmitter(timeout);
        CopyOnWriteArrayList<SseEmitter> emitters =
                connections.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);
        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(e -> removeEmitter(userId, emitter));
        return emitter;
    }

    public void send(Long userId, NotificationEvent event) {
        CopyOnWriteArrayList<SseEmitter> emitters = connections.get(userId);
        if (emitters == null || emitters.isEmpty()) return;
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .id(event.id())
                        .name(event.type())
                        .data(event));
            } catch (IOException e) {
                removeEmitter(userId, emitter);
            }
        }
    }

    public boolean isOnline(Long userId) {
        CopyOnWriteArrayList<SseEmitter> emitters = connections.get(userId);
        return emitters != null && !emitters.isEmpty();
    }

    private void removeEmitter(Long userId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = connections.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
        }
    }
}
