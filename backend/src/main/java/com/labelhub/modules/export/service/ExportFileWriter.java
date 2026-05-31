package com.labelhub.modules.export.service;

import com.labelhub.modules.export.dto.ExportFieldMapping;
import com.labelhub.modules.export.domain.ExportFormat;
import com.labelhub.modules.submission.dto.ExportableSubmissionSnapshot;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

/**
 * 导出文件写入器。
 *
 * <p>每种格式各自实现流式写入，避免一次性将全量导出快照载入内存。</p>
 */
public interface ExportFileWriter {

    ExportFormat format();

    String contentType();

    ExportFileWriteSession open(OutputStream outputStream, List<ExportFieldMapping> fieldMappings) throws IOException;

    interface ExportFileWriteSession extends AutoCloseable {

        void writeRow(ExportableSubmissionSnapshot snapshot) throws IOException;

        @Override
        void close() throws IOException;
    }
}
