package com.labelhub.modules.template.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 给 BE-A 使用的模板 schema 快照。
 */
public record TemplateSchemaResponse(Long versionId,
                                     Long templateId,
                                     Long ownerId,
                                     Integer versionNo,
                                     JsonNode schemaJson,
                                     Boolean publishedSnapshot) {
}
