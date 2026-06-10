package com.labelhub.modules.ai.service;

import java.util.Locale;

final class AiReviewDecisions {

    static final String PASS = "PASS";
    static final String REJECT = "REJECT";
    static final String MANUAL_REVIEW = "MANUAL_REVIEW";

    private AiReviewDecisions() {
    }

    static String normalizeForStorage(Object decision) {
        if (decision == null) {
            return MANUAL_REVIEW;
        }
        String normalized = String.valueOf(decision).trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case PASS -> PASS;
            case REJECT, "RETURN" -> REJECT;
            case MANUAL_REVIEW, "UNCERTAIN" -> MANUAL_REVIEW;
            default -> MANUAL_REVIEW;
        };
    }
}
