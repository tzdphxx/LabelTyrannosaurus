import { read, utils } from 'xlsx'
import type { DatasetSampleRow, ImportIssue, ImportPreview } from '../../types/import'

type DatasetFileType = ImportPreview['fileType']
type ParsedRow = Record<string, unknown>

const SAMPLE_LIMIT = 20

function createIssue(row: number, message: string, field = '-', level: ImportIssue['level'] = 'blocking'): ImportIssue {
  return {
    id: `issue-${row}-${field}`,
    row,
    field,
    level,
    message,
  }
}

function isPlainRecord(value: unknown): value is ParsedRow {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value)
}

function normalizeCellValue(value: unknown): DatasetSampleRow['values'][string] {
  if (value === undefined || value === null || value === '') {
    return null
  }

  if (typeof value === 'string' || typeof value === 'boolean') {
    return value
  }

  if (typeof value === 'number') {
    return Number.isFinite(value) ? value : null
  }

  return JSON.stringify(value)
}

function getOrderedFields(rows: ParsedRow[]) {
  const fields: string[] = []
  const fieldSet = new Set<string>()

  rows.forEach((row) => {
    Object.keys(row).forEach((field) => {
      if (!fieldSet.has(field)) {
        fieldSet.add(field)
        fields.push(field)
      }
    })
  })

  return fields
}

function toSampleRows(rows: ParsedRow[]): DatasetSampleRow[] {
  return rows.slice(0, SAMPLE_LIMIT).map((row, index) => ({
    id: `sample-${index + 1}`,
    values: Object.fromEntries(Object.entries(row).map(([field, value]) => [field, normalizeCellValue(value)])),
  }))
}

function buildImportPreview(file: File, fileType: DatasetFileType, rows: ParsedRow[], invalidRows: number, issues: ImportIssue[]): ImportPreview {
  const fields = getOrderedFields(rows)
  const nextIssues = [...issues]

  if (!rows.length && !nextIssues.some((issue) => issue.level === 'blocking')) {
    nextIssues.push(createIssue(1, '文件没有可展示的数据'))
  }

  return {
    id: `import-${Date.now()}`,
    fileName: file.name,
    fileType,
    totalRows: rows.length + invalidRows,
    validRows: rows.length,
    invalidRows,
    mappings: fields.map((field) => ({
      sourceField: field,
      targetField: field,
      required: false,
      matched: true,
    })),
    samples: toSampleRows(rows),
    issues: nextIssues,
  }
}

function rowsFromUnknownItems(items: unknown[], issues: ImportIssue[], rowOffset = 1) {
  const rows: ParsedRow[] = []
  let invalidRows = 0

  items.forEach((item, index) => {
    if (isPlainRecord(item)) {
      rows.push(item)
      return
    }

    invalidRows += 1
    issues.push(createIssue(index + rowOffset, '该行不是 JSON 对象'))
  })

  return { invalidRows, rows }
}

export async function parseJsonDatasetFile(file: File): Promise<ImportPreview> {
  const issues: ImportIssue[] = []

  try {
    const parsed = JSON.parse(await file.text()) as unknown

    if (!Array.isArray(parsed)) {
      return buildImportPreview(file, 'json', [], 1, [createIssue(1, 'JSON 顶层必须是对象数组')])
    }

    const { invalidRows, rows } = rowsFromUnknownItems(parsed, issues)
    return buildImportPreview(file, 'json', rows, invalidRows, issues)
  } catch {
    return buildImportPreview(file, 'json', [], 1, [createIssue(1, 'JSON 文件解析失败')])
  }
}

export async function parseJsonlDatasetFile(file: File): Promise<ImportPreview> {
  const issues: ImportIssue[] = []
  const rows: ParsedRow[] = []
  let invalidRows = 0

  const lines = (await file.text()).split(/\r?\n/)

  lines.forEach((line, index) => {
    const trimmedLine = line.trim()

    if (!trimmedLine) {
      return
    }

    try {
      const parsed = JSON.parse(trimmedLine) as unknown

      if (isPlainRecord(parsed)) {
        rows.push(parsed)
        return
      }

      invalidRows += 1
      issues.push(createIssue(index + 1, '该行不是 JSON 对象'))
    } catch {
      invalidRows += 1
      issues.push(createIssue(index + 1, '该行 JSON 解析失败'))
    }
  })

  return buildImportPreview(file, 'jsonl', rows, invalidRows, issues)
}

export async function parseExcelDatasetFile(file: File): Promise<ImportPreview> {
  try {
    const workbook = read(await file.arrayBuffer())
    const sheetName = workbook.SheetNames[0]

    if (!sheetName) {
      return buildImportPreview(file, 'excel', [], 1, [createIssue(1, 'Excel 文件没有工作表')])
    }

    const worksheet = workbook.Sheets[sheetName]
    const issues: ImportIssue[] = []
    const rows = utils.sheet_to_json<ParsedRow>(worksheet, { defval: null, raw: false })
    const { invalidRows, rows: validRows } = rowsFromUnknownItems(rows, issues)

    return buildImportPreview(file, 'excel', validRows, invalidRows, issues)
  } catch {
    return buildImportPreview(file, 'excel', [], 1, [createIssue(1, 'Excel 文件解析失败')])
  }
}
