package com.labelhub.modules.assignment.dto;

import com.labelhub.modules.task.domain.Strategy;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "标注市场任务响应")
public record MarketTaskResponse(
        @Schema(description = "任务ID") Long taskId,
        @Schema(description = "任务标题") String title,
        @Schema(description = "标签列表") List<String> tags,
        @Schema(description = "截止时间") LocalDateTime deadlineAt,
        @Schema(description = "当前用户可领取数") Integer availableCount,
        @Schema(description = "当前用户已认领数量") Integer currentUserClaimedCount,
        @Schema(description = "分发策略") Strategy strategy,
        @Schema(description = "当前标注员对此任务的状态：CAN_CLAIM / CLAIMED_SOME / UNAVAILABLE") String taskStatus,
        @Schema(description = "奖励摘要") RewardSummaryResponse rewardSummary) {
}
