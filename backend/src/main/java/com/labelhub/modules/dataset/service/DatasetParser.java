package com.labelhub.modules.dataset.service;

import com.labelhub.modules.dataset.domain.DatasetFileFormat;
import com.labelhub.modules.dataset.domain.DatasetType;

import java.io.InputStream;

public interface DatasetParser {

    DatasetFileFormat format();

    DatasetParseResult parse(InputStream inputStream, DatasetType datasetType);
}
