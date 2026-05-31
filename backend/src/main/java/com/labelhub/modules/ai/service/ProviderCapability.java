package com.labelhub.modules.ai.service;

public record ProviderCapability(
        boolean supportVision,
        boolean supportMultiImage,
        int maxImageCount,
        String visionModel
) {
    public static ProviderCapability textOnly() {
        return new ProviderCapability(false, false, 0, null);
    }
}
