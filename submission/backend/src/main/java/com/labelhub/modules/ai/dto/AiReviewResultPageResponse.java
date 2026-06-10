package com.labelhub.modules.ai.dto;

import java.util.List;

public record AiReviewResultPageResponse(List<AiReviewResultResponse> items,
                                         int page,
                                         int pageSize,
                                         long total) {
}
