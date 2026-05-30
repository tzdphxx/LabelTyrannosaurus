package com.labelhub.modules.ai.service;

import com.labelhub.infrastructure.llm.LlmMessage;
import java.util.List;

public record MediaPromptResult(
        List<LlmMessage> messages,
        PromptMode promptMode,
        boolean degraded,
        List<String> limitations,
        String promptSnapshot
) {
}
