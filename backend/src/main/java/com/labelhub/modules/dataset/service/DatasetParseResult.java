package com.labelhub.modules.dataset.service;

import java.util.List;

public record DatasetParseResult(List<DatasetImportRow> rows, List<DatasetImportError> errors) {
}
