package com.labelhub.modules.assignment.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "任务认领响应")
public record AssignmentClaimResponse(
        @Schema(description = "分配ID") Long assignmentId,
        @Schema(description = "数据集条目ID") Long datasetItemId,
        @Schema(description = "模板版本ID") Long templateVersionId,
        @Schema(description = "模板Schema JSON") String schemaJson,
        @Schema(description = "条目JSON") String itemJson,
        @Schema(description = "草稿答案JSON") String draftAnswerJson,
        @Schema(description = "草稿版本") Integer draftVersion) {
}
