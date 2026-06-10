package com.labelhub.modules.template.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * 创建模板请求。
 */
public record CreateTemplateRequest(@NotBlank String name,
                                    @NotNull Map<String, Object> schemaJson,
                                    String changeNote) {
}
