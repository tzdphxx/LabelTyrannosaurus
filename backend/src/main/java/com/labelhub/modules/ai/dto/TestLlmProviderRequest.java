package com.labelhub.modules.ai.dto;

import jakarta.validation.constraints.Size;
import java.util.Map;

public record TestLlmProviderRequest(
        @Size(max = 4096) String apiKey,
        @Size(max = 128) String modelName,
        Map<String, String> customHeaders
) {
}
