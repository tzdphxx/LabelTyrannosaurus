package com.labelhub.modules.template.service;

import java.io.Serial;
import java.io.Serializable;

public record TemplateSchemaSnapshot(Long templateVersionId, String schemaJson) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
