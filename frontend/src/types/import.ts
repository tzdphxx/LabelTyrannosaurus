export type ImportErrorLevel = 'warning' | 'blocking'

export interface DatasetFieldMapping {
  sourceField: string
  targetField: string
  required: boolean
  matched: boolean
}

export interface DatasetSampleRow {
  id: string
  values: Record<string, string | number | boolean | null>
}

export interface ImportIssue {
  id: string
  row: number
  field: string
  level: ImportErrorLevel
  message: string
}

export interface ImportPreview {
  id: string
  fileName: string
  fileType: 'json' | 'jsonl' | 'excel'
  totalRows: number
  validRows: number
  invalidRows: number
  mappings: DatasetFieldMapping[]
  samples: DatasetSampleRow[]
  issues: ImportIssue[]
}

export interface FileUploadResponse {
  fileId: number
  originalFilename: string
  contentType: string
  fileSize: number
  objectKey: string
  checksum: string
  downloadUrl: string
}

export type DatasetImportJobStatus = 'PENDING' | 'RUNNING' | 'SUCCESS' | 'FAILED' | 'PARTIAL_SUCCESS'

export interface DatasetImportJobResponse {
  jobId: number
  taskId: number
  status: DatasetImportJobStatus
  importMode: 'APPEND' | 'OVERWRITE'
  totalCount: number
  successCount: number
  failedCount: number
  errorReportFileId: number | null
  errorReportUrl: string | null
  errorMessage: string | null
  startedAt: string | null
  finishedAt: string | null
  createdAt: string
}

export interface BatchAppendByFileRequest {
  fileId: number
}
