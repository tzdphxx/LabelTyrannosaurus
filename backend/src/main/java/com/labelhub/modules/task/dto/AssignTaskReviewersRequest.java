package com.labelhub.modules.task.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AssignTaskReviewersRequest(
        @NotEmpty List<Long> reviewerIds
) {
}
