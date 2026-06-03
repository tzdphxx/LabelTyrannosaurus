package com.labelhub.modules.assignment.dto;

import com.labelhub.modules.assignment.domain.AssignmentStatus;
import java.time.LocalDateTime;

public record LabelerClaimedItemResponse(Long assignmentId,
                                         Long datasetItemId,
                                         AssignmentStatus assignmentStatus,
                                         String itemJson,
                                         String metadataJson,
                                         Integer draftVersion,
                                         String latestSubmissionStatus,
                                         LocalDateTime updatedAt) {
}
