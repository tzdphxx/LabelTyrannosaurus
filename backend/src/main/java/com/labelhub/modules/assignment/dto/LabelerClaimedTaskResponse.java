package com.labelhub.modules.assignment.dto;

import com.labelhub.modules.task.domain.TaskStatus;
import java.time.LocalDateTime;
import java.util.List;

public record LabelerClaimedTaskResponse(Long taskId,
                                         String title,
                                         String description,
                                         String instructionRichText,
                                         TaskStatus status,
                                         Integer quota,
                                         Integer overlapCount,
                                         LocalDateTime deadlineAt,
                                         Long publishedTemplateVersionId,
                                         Long claimedItemCount,
                                         LocalDateTime updatedAt,
                                         List<LabelerClaimedItemResponse> itemsPreview) {
}
