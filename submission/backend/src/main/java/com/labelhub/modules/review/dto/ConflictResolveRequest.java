package com.labelhub.modules.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConflictResolveRequest(
        @NotNull Long goldenSubmissionId,
        @NotBlank String reason
) {
}
