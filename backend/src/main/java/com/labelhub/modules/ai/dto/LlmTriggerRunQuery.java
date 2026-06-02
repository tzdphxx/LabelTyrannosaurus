package com.labelhub.modules.ai.dto;

import java.time.LocalDateTime;

public record LlmTriggerRunQuery(Long taskId,
                                 Integer page,
                                 Integer pageSize,
                                 String status,
                                 String componentId,
                                 LocalDateTime startTime,
                                 LocalDateTime endTime) {

    public int normalizedPage() {
        return page == null || page < 1 ? 1 : page;
    }

    public int normalizedPageSize() {
        if (pageSize == null || pageSize < 1) {
            return 20;
        }
        return Math.min(pageSize, 100);
    }

    public int offset() {
        return (normalizedPage() - 1) * normalizedPageSize();
    }
}
