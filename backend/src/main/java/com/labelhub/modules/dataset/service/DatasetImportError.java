package com.labelhub.modules.dataset.service;

import com.fasterxml.jackson.databind.JsonNode;

public record DatasetImportError(int rowNo,
                                 String externalId,
                                 String errorCode,
                                 String errorMessage,
                                 JsonNode rawRow) {
}
