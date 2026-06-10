package com.labelhub.modules.assignment.service;

import com.labelhub.modules.assignment.domain.AssignmentStatus;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

public record AssignmentDraftCacheEntry(Long assignmentId,
                                        Long labelerId,
                                        String draftAnswerJson,
                                        Integer draftVersion,
                                        AssignmentStatus status,
                                        LocalDateTime updatedAt) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
