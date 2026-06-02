package com.labelhub.modules.submission.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.labelhub.modules.submission.domain.SubmissionStatus;
import java.time.LocalDateTime;

@Schema(description = "版本历史条目")
public record VersionHistoryItem(
        @Schema(description = "提交ID") Long submissionId,
        @Schema(description = "版本号") Integer versionNo,
        @Schema(description = "提交状态") SubmissionStatus status,
        @Schema(description = "答案哈希") String answerHash,
        @Schema(description = "是否为黄金标准") Boolean isGolden,
        @Schema(description = "提交时间") LocalDateTime submittedAt,
        @Schema(description = "AI决策") String aiDecision,
        @Schema(description = "AI流程操作") String aiFlowAction,
        @Schema(description = "最新审核操作") String latestReviewAction
) {}
