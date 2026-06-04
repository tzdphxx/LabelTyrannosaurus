package com.labelhub.modules.assignment.dto;

public record LabelerTaskTemplateResponse(
        Long taskId,
        Long templateVersionId,
        String schemaJson
) {
}
