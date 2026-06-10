package com.labelhub.infrastructure.llm;

import java.util.Map;

/**
 * Describes the OpenAI {@code response_format} to request from the provider.
 *
 * <ul>
 *   <li>{@code NONE} — omit response_format entirely (legacy behaviour, text + fence parsing)</li>
 *   <li>{@code JSON_OBJECT} — request {@code {"type":"json_object"}} (JSON mode, broad compatibility)</li>
 *   <li>{@code JSON_SCHEMA} — request a strict {@code json_schema} (only when provider declares support
 *       and the caller supplies a schema)</li>
 * </ul>
 */
public record ResponseFormat(Mode mode, Map<String, Object> jsonSchema, String schemaName) {

    public enum Mode {
        NONE,
        JSON_OBJECT,
        JSON_SCHEMA
    }

    public static ResponseFormat none() {
        return new ResponseFormat(Mode.NONE, null, null);
    }

    public static ResponseFormat jsonObject() {
        return new ResponseFormat(Mode.JSON_OBJECT, null, null);
    }

    public static ResponseFormat jsonSchema(String name, Map<String, Object> schema) {
        return new ResponseFormat(Mode.JSON_SCHEMA, schema, name);
    }
}
