package com.labelhub.modules.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BatchRejectRequest(
        @NotEmpty List<Long> submissionIds,
        @NotBlank String reason,
        int reviewLevel
) {
}
