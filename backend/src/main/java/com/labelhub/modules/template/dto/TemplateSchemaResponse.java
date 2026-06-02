package com.labelhub.modules.template.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 给 BE-A 使用的模板 schema 快照。
 */
@Schema(description = "模板Schema快照，给BE-A使用")
public record TemplateSchemaResponse(
        @Schema(description = "版本ID") Long versionId,
        @Schema(description = "模板ID") Long templateId,
        @Schema(description = "任务ID") Long taskId,
        @Schema(description = "版本号") Integer versionNo,
        @Schema(description = "Schema JSON") JsonNode schemaJson,
        @Schema(description = "是否为发布快照") Boolean publishedSnapshot) {
}
