import { mockImportPreviews } from '../../mocks'
import type { FileUploadResponse, ImportPreview } from '../../types/import'
import { isRealServiceMode, request } from '../http'
import { parseExcelDatasetFile, parseJsonDatasetFile, parseJsonlDatasetFile } from './datasetFileParsers'

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

export const ownerImportService = {
  async uploadDatasetFile(file: File): Promise<FileUploadResponse> {
    if (isRealServiceMode()) {
      const formData = new FormData()
      formData.append('file', file)

      return request.post<FileUploadResponse, FormData>('/v1/files/upload', formData)
    }

    return {
      fileId: Date.now(),
      fileName: file.name,
      fileSize: file.size,
      contentType: file.type || 'application/octet-stream',
      businessType: 'DATASET',
      uploadedAt: new Date().toISOString(),
    }
  },

  async getDefaultImportPreview(): Promise<ImportPreview> {
    const preview = mockImportPreviews.find((item) => item.issues.every((issue) => issue.level !== 'blocking')) ?? mockImportPreviews[0]

    return cloneImportPreview(preview)
  },

  async parseDatasetFile(file: File): Promise<ImportPreview> {
    const extension = file.name.split('.').pop()?.toLowerCase()

    if (extension === 'json') {
      return parseJsonDatasetFile(file)
    }

    if (extension === 'jsonl') {
      return parseJsonlDatasetFile(file)
    }

    if (extension === 'xlsx') {
      return parseExcelDatasetFile(file)
    }

    return {
      id: `import-${Date.now()}`,
      fileName: file.name,
      fileType: 'json',
      totalRows: 0,
      validRows: 0,
      invalidRows: 1,
      mappings: [],
      samples: [],
      issues: [
        {
          id: 'issue-unsupported-file-type',
          row: 1,
          field: '-',
          level: 'blocking',
          message: '仅支持 JSON、JSONL 和 XLSX 文件',
        },
      ],
    }
  },

  async getImportPreview(importPreviewId: string): Promise<ImportPreview | null> {
    const preview = mockImportPreviews.find((item) => item.id === importPreviewId)

    return preview ? cloneImportPreview(preview) : null
  },
}
