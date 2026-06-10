package com.labelhub.modules.template.dto;

import java.time.LocalDateTime;

/**
 * 模板主表响应。
 */
public record TemplateResponse(Long templateId,
                               Long taskId,
                               Long ownerId,
                               String name,
                               Integer currentVersionNo,
                               TemplateVersionResponse currentVersion,
                               Long createdBy,
                               LocalDateTime createdAt,
                               LocalDateTime updatedAt) {
}
