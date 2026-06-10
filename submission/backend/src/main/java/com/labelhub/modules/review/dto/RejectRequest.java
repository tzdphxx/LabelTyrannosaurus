package com.labelhub.modules.review.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record RejectRequest(
        @NotBlank String reason,
        @Min(1) int reviewLevel
) {
}
