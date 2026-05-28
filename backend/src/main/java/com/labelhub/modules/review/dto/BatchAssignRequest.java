package com.labelhub.modules.review.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record BatchAssignRequest(
        @NotEmpty List<Long> submissionIds,
        @NotNull Long targetReviewerId
) {
}
