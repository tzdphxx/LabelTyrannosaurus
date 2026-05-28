package com.labelhub.modules.dataset.dto;

import com.labelhub.modules.dataset.domain.DatasetType;
import jakarta.validation.constraints.NotNull;

public record DatasetImportRequest(@NotNull Long fileId, @NotNull DatasetType datasetType) {
}
