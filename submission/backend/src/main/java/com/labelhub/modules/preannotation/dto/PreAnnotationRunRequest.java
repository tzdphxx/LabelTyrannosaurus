package com.labelhub.modules.preannotation.dto;

import jakarta.validation.constraints.Size;

public record PreAnnotationRunRequest(
        Long templateVersionId,
        Long datasetItemId,
        String currentAnswerJson,
        @Size(max = 30) String mode
) {
}
