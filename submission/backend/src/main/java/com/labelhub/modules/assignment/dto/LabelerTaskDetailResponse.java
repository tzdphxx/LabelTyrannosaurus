package com.labelhub.modules.assignment.dto;

import com.labelhub.modules.dataset.dto.ItemSummaryResponse;
import com.labelhub.modules.task.dto.TaskSummaryResponse;
import java.util.List;

public record LabelerTaskDetailResponse(
        TaskSummaryResponse task,
        String description,
        String instructionRichText,
        Long templateVersionId,
        Integer availableCount,
        Integer currentUserClaimedCount,
        RewardSummaryResponse rewardSummary,
        List<ItemSummaryResponse> items
) {
}
