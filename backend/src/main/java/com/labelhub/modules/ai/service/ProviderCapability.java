package com.labelhub.modules.ai.service;

public record ProviderCapability(
        boolean supportVision,
        boolean supportMultiImage,
        int maxImageCount,
        String visionModel,
        String structuredOutputMode,
        String outputSchemaJson
) {
    public ProviderCapability(boolean supportVision, boolean supportMultiImage,
                              int maxImageCount, String visionModel) {
        this(supportVision, supportMultiImage, maxImageCount, visionModel, "NONE", null);
    }

    public ProviderCapability(boolean supportVision, boolean supportMultiImage,
                              int maxImageCount, String visionModel,
                              String structuredOutputMode) {
        this(supportVision, supportMultiImage, maxImageCount, visionModel, structuredOutputMode, null);
    }

    public static ProviderCapability textOnly() {
        return new ProviderCapability(false, false, 0, null, "NONE", null);
    }
}
