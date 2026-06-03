package com.labelhub.modules.task.dto;

import com.labelhub.modules.ai.dto.AiReviewConfigResponse;
import com.labelhub.modules.ai.dto.LlmProviderResponse;
import com.labelhub.modules.task.domain.TaskStatus;
import java.time.LocalDateTime;
import java.util.List;

public record TaskDetailResponse(
        Long taskId,
        Long ownerId,
        String title,
        String description,
        String instructionRichText,
        TaskStatus status,
        List<String> tags,
        Integer quota,
        Integer claimedCount,
        Integer overlapCount,
        LocalDateTime deadlineAt,
        Long publishedTemplateVersionId,
        Long aiReviewConfigId,
        Integer reviewLevelCount,
        Boolean rewardVisible,
        LocalDateTime publishedAt,
        LocalDateTime endedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LlmProviderResponse aiProvider,
        AiReviewConfigResponse aiReviewConfig
) {
}
