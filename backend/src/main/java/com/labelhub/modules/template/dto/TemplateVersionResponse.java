package com.labelhub.modules.template.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.labelhub.modules.template.domain.TemplateVersionState;

import java.time.LocalDateTime;

/**
 * 模板版本响应。
 */
public record TemplateVersionResponse(Long versionId,
                                      Long templateId,
                                      Long taskId,
                                      Integer versionNo,
                                      JsonNode schemaJson,
                                      Boolean publishedSnapshot,
                                      TemplateVersionState state,
                                      String changeNote,
                                      Long createdBy,
                                      LocalDateTime createdAt) {
}
