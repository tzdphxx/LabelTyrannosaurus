import { mockImportPreviews } from '../../mocks'
import type { BatchAppendByFileRequest, DatasetImportJobResponse, FileUploadResponse, ImportPreview } from '../../types/import'

function cloneImportPreview(preview: ImportPreview): ImportPreview {
  return {
    ...preview,
    mappings: preview.mappings.map((mapping) => ({ ...mapping })),
    samples: preview.samples.map((sample) => ({
      ...sample,
      values: { ...sample.values },
    })),
    issues: preview.issues.map((issue) => ({ ...issue })),
  }
}

function nowIso() {
  return new Date().toISOString()
}

export const ownerImportService = {
  async uploadDatasetFile(file: File): Promise<FileUploadResponse> {
    return {
      fileId: Date.now(),
      originalFilename: file.name,
      contentType: file.type || 'application/octet-stream',
      fileSize: file.size,
      objectKey: `uploads/dataset/mock/${file.name}`,
      checksum: 'mock-checksum',
      downloadUrl: '',
    }
  },

  async appendDatasetItemsByFile(taskId: string | number, request: BatchAppendByFileRequest): Promise<DatasetImportJobResponse> {
    return {
      jobId: Date.now(),
      taskId: Number(taskId),
      status: 'PENDING',
      importMode: 'APPEND',
      totalCount: 0,
      successCount: 0,
      failedCount: 0,
      errorReportFileId: null,
      errorReportUrl: null,
      errorMessage: request.fileId > 0 ? null : 'Invalid fileId',
      startedAt: null,
      finishedAt: null,
      createdAt: nowIso(),
    }
  },

  async getDefaultImportPreview(): Promise<ImportPreview> {
    const preview = mockImportPreviews.find((item) => item.issues.every((issue) => issue.level !== 'blocking')) ?? mockImportPreviews[0]

    return cloneImportPreview(preview)
  },

  async getImportPreview(importPreviewId: string): Promise<ImportPreview | null> {
    const preview = mockImportPreviews.find((item) => item.id === importPreviewId)

    return preview ? cloneImportPreview(preview) : null
  },
}
