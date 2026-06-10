package com.labelhub.modules.ai.tool;

public record ToolContext(
        Long submissionId,
        Long taskId,
        Long datasetItemId,
        Long labelerId,
        String answerJson,
        String itemJson
) {
}
