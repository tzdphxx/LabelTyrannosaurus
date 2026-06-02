package com.labelhub.modules.template.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * fork 模板版本请求。
 *
 * @param baseVersionId 可选基准版本；为空时使用模板当前版本
 * @param schemaJson 可选新 schema；为空时复制基准版本 schema
 * @param changeNote 版本说明
 */
@Schema(description = "Fork模板版本请求")
public record ForkTemplateRequest(
        @Schema(description = "基准版本ID，为空时使用模板当前版本") Long baseVersionId,
        @Schema(description = "新的Schema JSON，为空时复制基准版本") Map<String, Object> schemaJson,
        @Schema(description = "版本说明") String changeNote) {
}
