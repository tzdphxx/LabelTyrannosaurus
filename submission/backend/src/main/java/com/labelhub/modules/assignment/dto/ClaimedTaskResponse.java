package com.labelhub.modules.assignment.dto;

import com.labelhub.modules.task.dto.TaskSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "已领取任务视图")
public record ClaimedTaskResponse(
        @Schema(description = "任务摘要")
        TaskSummaryResponse task,
        @Schema(description = "当前用户在该任务下的已领取数", example = "5")
        Integer myClaimedCount,
        @Schema(description = "当前用户在该任务下的已提交数", example = "3")
        Integer mySubmittedCount,
        @Schema(description = "当前用户在该任务下的已通过数", example = "2")
        Integer myApprovedCount,
        @Schema(description = "已领取题目列表")
        List<ClaimedItemResponse> items
) {}
