package com.labelhub.modules.template.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 模板主表响应。
 */
@Schema(description = "模板主表响应")
public record TemplateResponse(
        @Schema(description = "模板ID") Long templateId,
        @Schema(description = "任务ID") Long taskId,
        @Schema(description = "模板名称") String name,
        @Schema(description = "当前版本号") Integer currentVersionNo,
        @Schema(description = "当前版本详情") TemplateVersionResponse currentVersion,
        @Schema(description = "创建人ID") Long createdBy,
        @Schema(description = "创建时间") LocalDateTime createdAt,
        @Schema(description = "更新时间") LocalDateTime updatedAt) {
}
