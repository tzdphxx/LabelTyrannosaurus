package com.labelhub.modules.export.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 导出字段映射。
 *
 * <p>`sourceJsonPath` 采用轻量 JSONPath 语法，只支持对象属性访问，不支持过滤器和函数。</p>
 */
@Schema(description = "导出字段映射")
public record ExportFieldMapping(
        @Schema(description = "源JSON路径，轻量JSONPath语法") String sourceJsonPath,
        @Schema(description = "目标字段名") String targetName,
        @Schema(description = "格式化器") String formatter,
        @Schema(description = "是否包含") Boolean include) {
}
