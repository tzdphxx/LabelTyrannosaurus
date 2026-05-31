package com.labelhub.modules.ai.service;

import com.labelhub.modules.ai.dto.LlmProviderTestResponse;

public interface LlmProviderTester {

    LlmProviderTestResponse test(LlmProviderRuntimeConfig config);
}
