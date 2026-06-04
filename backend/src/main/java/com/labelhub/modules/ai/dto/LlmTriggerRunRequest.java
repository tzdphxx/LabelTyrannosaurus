package com.labelhub.modules.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Map;

@Schema(description = "LlmTrigger run request. Labeler clicks a component and backend builds LLM context.")
public record LlmTriggerRunRequest(
        @Schema(description = "Legacy provider id. Ignored by labeler run; kept for old clients.")
        Long providerId,
        @Schema(description = "Legacy model name. Ignored by labeler run; kept for old clients.")
        @Size(max = 128) String modelName,
        @Schema(description = "Legacy prompt template. Ignored by labeler run; kept for old clients.")
        @Size(max = 10000) String promptTemplate,
        @Schema(description = "Legacy target fields. Ignored by labeler run; kept for old clients.")
        List<@Size(max = 64) String> targetFields,
        @Schema(description = "Dataset item id for owner preview.")
        Long datasetItemId,
        @Schema(description = "Clicked template component id.", example = "summary")
        @Size(max = 128) String componentId,
        @Schema(description = "Current draft answer JSON.")
        Map<String, Object> currentAnswerJson,
        @Schema(description = "Optional extra instruction from the labeler/owner.")
        @Size(max = 1000) String userInstruction
) {
}
