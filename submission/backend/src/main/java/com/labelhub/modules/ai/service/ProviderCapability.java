package com.labelhub.modules.ai.service;

public record ProviderCapability(
        boolean supportVision,
        boolean supportMultiImage,
        int maxImageCount,
        String visionModel,
        String structuredOutputMode
) {
    public ProviderCapability(boolean supportVision, boolean supportMultiImage,
                              int maxImageCount, String visionModel) {
        this(supportVision, supportMultiImage, maxImageCount, visionModel, "NONE");
    }

    public static ProviderCapability textOnly() {
        return new ProviderCapability(false, false, 0, null, "NONE");
    }
}
