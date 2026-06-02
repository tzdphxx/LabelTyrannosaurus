package com.labelhub.modules.submission.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.labelhub.modules.submission.domain.SubmissionStatus;

@Schema(description = "提交响应")
public record SubmissionSubmitResponse(
        @Schema(description = "提交ID") Long submissionId,
        @Schema(description = "分配ID") Long assignmentId,
        @Schema(description = "版本号") Integer versionNo,
        @Schema(description = "提交状态") SubmissionStatus status,
        @Schema(description = "答案哈希") String answerHash,
        @Schema(description = "智能体运行ID") Long agentRunId) {
}
