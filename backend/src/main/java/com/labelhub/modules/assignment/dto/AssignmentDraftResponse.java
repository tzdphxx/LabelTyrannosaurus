package com.labelhub.modules.assignment.dto;

import com.labelhub.modules.assignment.domain.AssignmentStatus;
import java.time.LocalDateTime;

public record AssignmentDraftResponse(Long assignmentId,
                                      String draftAnswerJson,
                                      Integer draftVersion,
                                      AssignmentStatus status,
                                      LocalDateTime updatedAt) {
}
