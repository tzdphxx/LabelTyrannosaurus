package com.labelhub.modules.assignment.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record AssignmentClaimRequest(
        @Min(1)
        @Max(100)
        Integer quantity) {

    public int resolvedQuantity() {
        return quantity != null ? quantity : 1;
    }
}
