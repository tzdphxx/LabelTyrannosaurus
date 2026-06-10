package com.labelhub.modules.review.dto;

public record BatchReviewItemResult(
        Long submissionId,
        boolean success,
        String error
) {
    public static BatchReviewItemResult ok(Long submissionId) {
        return new BatchReviewItemResult(submissionId, true, null);
    }

    public static BatchReviewItemResult fail(Long submissionId, String error) {
        return new BatchReviewItemResult(submissionId, false, error);
    }
}
