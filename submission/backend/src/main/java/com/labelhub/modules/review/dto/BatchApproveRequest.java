package com.labelhub.modules.review.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BatchApproveRequest(
        @NotEmpty List<Long> submissionIds,
        String reviewComment,
        int reviewLevel
) {
}
