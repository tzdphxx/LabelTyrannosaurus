package com.labelhub.modules.assignment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "已领取题目")
public record ClaimedItemResponse(
        @Schema(description = "领取 ID", example = "200")
        Long claimId,
        @Schema(description = "题目 ID", example = "100")
        Long itemId,
        @Schema(description = "题目业务编号")
        String externalId,
        @Schema(description = "领取状态", example = "CLAIMED")
        String claimStatus,
        @Schema(description = "题目内容 JSON")
        String itemJson,
        @Schema(description = "题目元数据 JSON")
        String metadataJson,
        @Schema(description = "草稿版本号", example = "3")
        Integer draftVersion,
        @Schema(description = "最新提交状态")
        String latestSubmissionStatus,
        @Schema(description = "更新时间")
        LocalDateTime updatedAt
) {}
