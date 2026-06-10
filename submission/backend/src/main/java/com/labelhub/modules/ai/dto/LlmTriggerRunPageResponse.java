package com.labelhub.modules.ai.dto;

import java.util.List;

public record LlmTriggerRunPageResponse(List<LlmTriggerRunResponse> items,
                                        int page,
                                        int pageSize,
                                        long total) {
}
