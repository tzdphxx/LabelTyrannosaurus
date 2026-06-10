package com.labelhub.modules.ai.service;

import java.util.List;
import java.util.Map;

/**
 * JSON schema for the AI review structured output, used with OpenAI {@code json_schema}
 * response_format when a provider declares JSON_SCHEMA support.
 *
 * <p>{@code dimensionScores} uses {@code additionalProperties:true} so dynamic scoring
 * dimension keys are not locked down by strict mode.
 */
public final class AiReviewSchema {

    public static final String NAME = "ai_review";

    public static final Map<String, Object> SCHEMA = buildSchema();

    private AiReviewSchema() {
    }

    private static Map<String, Object> buildSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "decision", Map.of("type", "string", "enum", List.of(
                                AiReviewDecisions.PASS,
                                AiReviewDecisions.REJECT,
                                AiReviewDecisions.MANUAL_REVIEW)),
                        "averageScore", Map.of("type", "number"),
                        "confidence", Map.of("type", "number"),
                        "dimensionScores", Map.of("type", "object", "additionalProperties", true),
                        "riskFlags", Map.of("type", "array", "items", Map.of("type", "string")),
                        "suggestion", Map.of("type", "string"),
                        "limitations", Map.of("type", "array", "items", Map.of("type", "string"))
                ),
                "required", List.of("decision"),
                "additionalProperties", true
        );
    }
}
