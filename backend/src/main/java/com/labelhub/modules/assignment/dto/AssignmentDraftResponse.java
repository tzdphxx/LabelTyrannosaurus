package com.labelhub.modules.assignment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.labelhub.modules.assignment.domain.AssignmentStatus;
import java.time.LocalDateTime;

@Schema(description = "标注草稿响应")
public record AssignmentDraftResponse(
        @Schema(description = "分配ID") Long assignmentId,
        @Schema(description = "草稿答案JSON") String draftAnswerJson,
        @Schema(description = "草稿版本") Integer draftVersion,
        @Schema(description = "分配状态") AssignmentStatus status,
        @Schema(description = "更新时间") LocalDateTime updatedAt) {
}
