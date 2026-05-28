package com.labelhub.modules.submission.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubmissionSubmitRequest(@NotBlank String answerJson,
                                      @NotNull Integer clientVersion) {
}
