package com.labelhub.modules.export.dto;

/**
 * 导出字段映射。
 *
 * <p>`sourceJsonPath` 采用轻量 JSONPath 语法，只支持对象属性访问，不支持过滤器和函数。</p>
 */
public record ExportFieldMapping(String sourceJsonPath,
                                 String targetName,
                                 String formatter,
                                 Boolean include) {
}
