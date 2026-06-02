package com.labelhub.modules.template.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * 创建模板请求。
 */
@Schema(description = "创建模板请求")
public record CreateTemplateRequest(
        @NotBlank @Schema(description = "模板名称") String name,
        @NotNull @Schema(description = "Schema JSON定义") Map<String, Object> schemaJson,
        @Schema(description = "变更说明") String changeNote) {
}
