package com.labelhub.modules.media.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record MediaContextResponse(
        Long datasetItemId,
        Long taskId,
        String mediaType,
        String processingStatus,
        Map<String, Object> context,
        List<String> limitations,
        LocalDateTime updatedAt
) {
}
