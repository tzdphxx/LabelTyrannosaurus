package com.labelhub.modules.dataset.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "题目详情")
public record ItemResponse(
        @Schema(description = "题目 ID", example = "100")
        Long itemId,
        @Schema(description = "所属任务 ID", example = "10")
        Long taskId,
        @Schema(description = "题目业务编号", example = "q1")
        String externalId,
        @Schema(description = "题目内容 JSON")
        JsonNode itemJson,
        @Schema(description = "题目元数据 JSON")
        JsonNode metadataJson,
        @Schema(description = "已分配数", example = "1")
        Integer assignedCount,
        @Schema(description = "已提交数", example = "1")
        Integer submittedCount,
        @Schema(description = "已通过数", example = "1")
        Integer approvedCount,
        @Schema(description = "题目状态", example = "UNCLAIMED")
        ItemStatus itemStatus,
        @Schema(description = "当前有效标注员 ID")
        Long labelerId,
        @Schema(description = "创建时间")
        LocalDateTime createdAt,
        @Schema(description = "更新时间")
        LocalDateTime updatedAt
) {}
