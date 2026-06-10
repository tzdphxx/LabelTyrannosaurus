package com.labelhub.infrastructure.llm;

import java.util.List;

public record LlmGatewayRequest(Long providerId, String modelName, List<LlmMessage> messages,
                                ResponseFormat responseFormat) {

    public LlmGatewayRequest(Long providerId, String modelName, List<LlmMessage> messages) {
        this(providerId, modelName, messages, null);
    }
}
