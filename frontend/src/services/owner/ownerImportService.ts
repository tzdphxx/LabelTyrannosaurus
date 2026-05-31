import { mockImportPreviews } from '../../mocks'
import type { ImportPreview } from '../../types/import'

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
  async getDefaultImportPreview(): Promise<ImportPreview> {
    const preview = mockImportPreviews.find((item) => item.issues.every((issue) => issue.level !== 'blocking')) ?? mockImportPreviews[0]

    return cloneImportPreview(preview)
  },

  async getImportPreview(importPreviewId: string): Promise<ImportPreview | null> {
    const preview = mockImportPreviews.find((item) => item.id === importPreviewId)

    return preview ? cloneImportPreview(preview) : null
  },
}
