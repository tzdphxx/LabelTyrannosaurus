package com.labelhub.infrastructure.llm;

public interface LlmGateway {

    LlmGatewayResponse review(LlmGatewayRequest request);
}
