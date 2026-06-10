package com.labelhub.modules.ai.service;

public record MediaPromptInput(
        String itemJson,
        String answerJson,
        String promptTemplate,
        ProviderCapability providerCapability,
        boolean multimodalEnabled,
        String visionDetail,
        int maxImagesPerRequest
) {
}
