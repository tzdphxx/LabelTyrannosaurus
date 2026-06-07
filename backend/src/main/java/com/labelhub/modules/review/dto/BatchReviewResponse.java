package com.labelhub.modules.review.dto;

import java.util.List;

public record BatchReviewResponse(
        int total,
        int successCount,
        int failCount,
        List<BatchReviewItemResult> results
) {
}
