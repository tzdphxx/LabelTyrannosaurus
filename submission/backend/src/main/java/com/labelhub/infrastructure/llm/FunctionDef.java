package com.labelhub.infrastructure.llm;

import java.util.Map;

public record FunctionDef(String name, String description, Map<String, Object> parameters) {
}
