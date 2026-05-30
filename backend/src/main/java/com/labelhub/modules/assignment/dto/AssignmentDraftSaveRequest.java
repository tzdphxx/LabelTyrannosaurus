package com.labelhub.modules.assignment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AssignmentDraftSaveRequest(@NotBlank String answerJson,
                                         @NotNull Integer clientVersion) {
}
