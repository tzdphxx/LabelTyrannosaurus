package com.labelhub.infrastructure.notification;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Map;

@Schema(description = "通知事件")
public record NotificationEvent(
        @Schema(description = "通知 ID")
        String id,
        @Schema(description = "通知类型", example = "SUBMISSION_REVIEWED")
        String type,
        @Schema(description = "接收用户 ID")
        Long userId,
        @Schema(description = "通知标题")
        String title,
        @Schema(description = "通知正文")
        String body,
        @Schema(description = "附加数据")
        Map<String, Object> data,
        @Schema(description = "创建时间")
        LocalDateTime createdAt,
        @Schema(description = "是否已读")
        boolean read
) {
    public NotificationEvent withRead(boolean read) {
        return new NotificationEvent(id, type, userId, title, body, data, createdAt, read);
    }
}
