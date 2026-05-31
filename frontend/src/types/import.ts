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
