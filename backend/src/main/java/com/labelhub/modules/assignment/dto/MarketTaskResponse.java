package com.labelhub.modules.assignment.dto;

import java.time.LocalDateTime;
import java.util.List;
import com.labelhub.modules.task.domain.TaskStatus;

public record MarketTaskResponse(Long taskId,
                                 String title,
                                 List<String> tags,
                                 LocalDateTime deadlineAt,
                                 Integer availableCount,
                                 Integer currentUserClaimedCount,
                                 RewardSummaryResponse rewardSummary,
                                 String description,
                                 String instructionRichText,
                                 TaskStatus status,
                                 Integer quota,
                                 Integer overlapCount,
                                 Long publishedTemplateVersionId,
                                 List<MarketDatasetItemResponse> itemsPreview) {

    public MarketTaskResponse(Long taskId,
                              String title,
                              List<String> tags,
                              LocalDateTime deadlineAt,
                              Integer availableCount,
                              Integer currentUserClaimedCount,
                              RewardSummaryResponse rewardSummary) {
        this(taskId, title, tags, deadlineAt, availableCount, currentUserClaimedCount, rewardSummary,
                null, null, null, null, null, null, List.of());
    }
}
