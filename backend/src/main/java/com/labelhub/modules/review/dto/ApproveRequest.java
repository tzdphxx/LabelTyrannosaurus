package com.labelhub.modules.review.dto;

import jakarta.validation.constraints.Min;

public record ApproveRequest(
        String reviewComment,
        @Min(1) int reviewLevel
) {
}
