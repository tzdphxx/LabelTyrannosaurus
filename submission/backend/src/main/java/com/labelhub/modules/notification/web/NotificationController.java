package com.labelhub.modules.notification.web;

import com.labelhub.common.api.ApiResponse;
import com.labelhub.common.security.CurrentUserContext;
import com.labelhub.infrastructure.notification.NotificationEvent;
import com.labelhub.infrastructure.notification.NotificationService;
import com.labelhub.infrastructure.notification.NotificationStore;
import com.labelhub.infrastructure.notification.SseConnectionManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "通知推送", description = "SSE 实时通知和历史通知管理")
public class NotificationController {

    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;

    private final SseConnectionManager sseConnectionManager;
    private final NotificationStore notificationStore;

    public NotificationController(SseConnectionManager sseConnectionManager,
                                  NotificationStore notificationStore) {
        this.sseConnectionManager = sseConnectionManager;
        this.notificationStore = notificationStore;
    }

    @GetMapping("/stream")
    @Operation(summary = "SSE 连接", description = "建立 SSE 长连接接收实时通知。")
    public SseEmitter stream() {
        Long userId = CurrentUserContext.getUserId();
        return sseConnectionManager.createConnection(userId, SSE_TIMEOUT);
    }

    @GetMapping
    @Operation(summary = "历史通知列表")
    public ApiResponse<List<NotificationEvent>> list(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset) {
        Long userId = CurrentUserContext.getUserId();
        return ApiResponse.ok(notificationStore.findByUserId(userId, limit, offset));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "未读通知数量")
    public ApiResponse<Map<String, Long>> unreadCount() {
        Long userId = CurrentUserContext.getUserId();
        return ApiResponse.ok(Map.of("count", notificationStore.countUnread(userId)));
    }

    @PostMapping("/{id}/read")
    @Operation(summary = "标记单条已读")
    public ApiResponse<Void> markRead(@PathVariable String id) {
        Long userId = CurrentUserContext.getUserId();
        notificationStore.markRead(userId, id);
        return ApiResponse.ok(null);
    }

    @PostMapping("/read-all")
    @Operation(summary = "全部标记已读")
    public ApiResponse<Void> markAllRead() {
        Long userId = CurrentUserContext.getUserId();
        notificationStore.markAllRead(userId);
        return ApiResponse.ok(null);
    }
}