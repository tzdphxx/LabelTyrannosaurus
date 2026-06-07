package com.labelhub.modules.ai.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.Map;

public record AiReviewPromptTestRequest(
        @NotEmpty Map<String, Object> itemSnapshot,
        @NotEmpty Map<String, Object> answerJson
) {
}
