package com.labelhub.modules.template.dto;

import java.util.Map;

/**
 * fork 模板版本请求。
 *
 * @param baseVersionId 可选基准版本；为空时使用模板当前版本
 * @param schemaJson 可选新 schema；为空时复制基准版本 schema
 * @param changeNote 版本说明
 */
public record ForkTemplateRequest(Long baseVersionId,
                                  Map<String, Object> schemaJson,
                                  String changeNote) {
}
