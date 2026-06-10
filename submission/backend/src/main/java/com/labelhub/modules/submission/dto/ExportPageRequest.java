package com.labelhub.modules.submission.dto;

/**
 * BE-B 调用 BE-A 导出快照查询时使用的分页参数。
 *
 * <p>采用 submissionId 游标分页，避免大文件导出时一次性加载全量提交。</p>
 */
public record ExportPageRequest(Long afterSubmissionId,
                                int pageSize,
                                boolean includeAiReview,
                                boolean includeAuditTrail,
                                boolean includeReviewComment,
                                boolean includeLabelerInfo) {
}
