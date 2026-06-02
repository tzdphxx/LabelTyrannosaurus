package com.labelhub.modules.media.dto;

import java.time.LocalDateTime;

public record MediaProcessingJobResponse(
        Long jobId,
        Long datasetItemId,
        Long taskId,
        String status,
        Integer totalAssets,
        Integer processedAssets,
        String errorMessage,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        LocalDateTime createdAt
) {
}
