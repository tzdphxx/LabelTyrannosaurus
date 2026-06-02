package com.labelhub.modules.template.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.labelhub.modules.template.domain.TemplateVersionState;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 模板版本响应。
 */
@Schema(description = "模板版本响应")
public record TemplateVersionResponse(
        @Schema(description = "版本ID") Long versionId,
        @Schema(description = "模板ID") Long templateId,
        @Schema(description = "任务ID") Long taskId,
        @Schema(description = "版本号") Integer versionNo,
        @Schema(description = "Schema JSON") JsonNode schemaJson,
        @Schema(description = "是否为发布快照") Boolean publishedSnapshot,
        @Schema(description = "版本状态") TemplateVersionState state,
        @Schema(description = "变更说明") String changeNote,
        @Schema(description = "创建人ID") Long createdBy,
        @Schema(description = "创建时间") LocalDateTime createdAt) {
}
