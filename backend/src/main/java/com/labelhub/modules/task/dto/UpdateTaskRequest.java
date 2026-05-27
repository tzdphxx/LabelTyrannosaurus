package com.labelhub.modules.task.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

public record UpdateTaskRequest(
        @NotBlank
        @Size(max = 200)
        String title,
        String description,
        String instructionRichText,
        List<@Size(max = 64) String> tags,
        @NotNull
        @Min(1)
        Integer quota,
        @NotNull
        @Future
        LocalDateTime deadlineAt,
        @NotNull
        @Min(1)
        Integer overlapCount
) {
}
