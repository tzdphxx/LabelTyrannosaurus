package com.labelhub.modules.review.dto;

public record ExportPageRequest(
        Long taskId,
        Long lastId,
        int limit
) {
    public ExportPageRequest {
        if (limit <= 0 || limit > 200) {
            limit = 50;
        }
        if (lastId == null) {
            lastId = 0L;
        }
    }
}
