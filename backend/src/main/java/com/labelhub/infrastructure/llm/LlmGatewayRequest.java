package com.labelhub.infrastructure.llm;

import java.util.List;

public record LlmGatewayRequest(Long providerId, String modelName, List<LlmMessage> messages) {
}
