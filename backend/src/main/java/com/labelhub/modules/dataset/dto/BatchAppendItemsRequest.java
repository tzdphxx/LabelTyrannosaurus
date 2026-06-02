package com.labelhub.modules.dataset.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Batch append dataset items from an uploaded source file.
 */
public record BatchAppendItemsRequest(@NotNull Long fileId) {
}
