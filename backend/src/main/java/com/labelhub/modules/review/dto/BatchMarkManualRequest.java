package com.labelhub.modules.review.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BatchMarkManualRequest(
        @NotEmpty List<Long> submissionIds
) {
}
