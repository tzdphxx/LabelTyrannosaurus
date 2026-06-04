package com.labelhub.modules.dataset.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

/**
 * Request item for appending one dataset item from direct JSON content.
 */
public record DatasetItemAppendRequest(
        @NotBlank String externalId,
        @NotNull Map<String, Object> itemJson,
        Map<String, Object> metadataJson
) {
}
