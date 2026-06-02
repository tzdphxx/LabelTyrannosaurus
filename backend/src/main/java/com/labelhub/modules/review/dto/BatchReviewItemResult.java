package com.labelhub.modules.review.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "批量审核条目结果")
public record BatchReviewItemResult(
        @Schema(description = "提交ID") Long submissionId,
        @Schema(description = "是否成功") boolean success,
        @Schema(description = "错误信息") String error
) {
    public static BatchReviewItemResult ok(Long submissionId) {
        return new BatchReviewItemResult(submissionId, true, null);
    }

    public static BatchReviewItemResult fail(Long submissionId, String error) {
        return new BatchReviewItemResult(submissionId, false, error);
    }
}
