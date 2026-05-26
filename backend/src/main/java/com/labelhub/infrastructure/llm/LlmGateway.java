package com.labelhub.infrastructure.llm;

import java.util.Map;

public interface LlmGateway {

    Map<String, Object> review(Map<String, Object> request);
}
