package com.labelhub.modules.dataset.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Batch append dataset items from direct JSON request content.
 */
public record BatchAppendJsonItemsRequest(
        @NotEmpty List<@NotNull @Valid DatasetItemAppendRequest> items
) {
}
