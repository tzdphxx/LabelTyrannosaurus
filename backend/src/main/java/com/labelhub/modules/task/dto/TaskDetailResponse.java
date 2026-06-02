package com.labelhub.modules.task.dto;

import com.labelhub.modules.task.domain.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "任务详情响应")
public record TaskDetailResponse(
        @Schema(description = "任务 ID", example = "100")
        Long taskId,
        @Schema(description = "发布者 ID", example = "10")
        Long ownerId,
        @Schema(description = "任务标题", example = "图像分类标注任务")
        String title,
        @Schema(description = "任务描述", example = "对商品图片进行类别标注")
        String description,
        @Schema(description = "富文本标注说明")
        String instructionRichText,
        @Schema(description = "任务状态", example = "PUBLISHED")
        TaskStatus status,
        @Schema(description = "任务标签列表", example = "[\"image\", \"classification\"]")
        List<String> tags,
        @Schema(description = "任务配额", example = "100")
        Integer quota,
        @Schema(description = "已被领取的数量", example = "45")
        Integer claimedCount,
        @Schema(description = "每条数据需要的标注份数", example = "3")
        Integer overlapCount,
        @Schema(description = "截止时间", example = "2026-06-30T23:59:59")
        LocalDateTime deadlineAt,
        @Schema(description = "已发布模板版本 ID", example = "20")
        Long publishedTemplateVersionId,
        @Schema(description = "AI 审核配置 ID", example = "30")
        Long aiReviewConfigId,
        @Schema(description = "审核级别数", example = "3")
        Integer reviewLevelCount,
        @Schema(description = "奖励是否可见", example = "true")
        Boolean rewardVisible,
        @Schema(description = "发布时间", example = "2026-05-01T10:00:00")
        LocalDateTime publishedAt,
        @Schema(description = "结束时间", example = "2026-06-30T23:59:59")
        LocalDateTime endedAt,
        @Schema(description = "创建时间", example = "2026-04-15T08:30:00")
        LocalDateTime createdAt,
        @Schema(description = "更新时间", example = "2026-05-01T10:00:00")
        LocalDateTime updatedAt
) {
}
