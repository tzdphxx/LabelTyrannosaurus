package com.labelhub.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Shared utility for canonicalizing answer JSON and computing its SHA-256 hash.
 * Used by both submission and review paths to ensure consistent hashing.
 */
public final class AnswerCanonicalizer {

    private AnswerCanonicalizer() {
    }

    /**
     * Canonicalize answer JSON by parsing and re-serializing via Jackson,
     * ensuring consistent whitespace and key ordering.
     *
     * @throws com.labelhub.common.exception.BusinessException if the JSON is invalid
     */
    public static String canonicalize(String answerJson, ObjectMapper objectMapper) {
        try {
            JsonNode jsonNode = objectMapper.readTree(answerJson);
            return objectMapper.writeValueAsString(jsonNode);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Answer JSON is invalid: " + ex.getMessage(), ex);
        }
    }

    /**
     * Compute SHA-256 hex digest of a string.
     */
    public static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
