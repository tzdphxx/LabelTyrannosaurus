package com.labelhub.modules.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record LlmTriggerRunRequest(
        @NotNull Long taskId,
        @NotNull Long templateVersionId,
        @NotBlank String componentId,
        Long datasetItemId,
        Long assignmentId,
        Map<String, Object> currentAnswerJson,
        boolean previewMode
) {
}
