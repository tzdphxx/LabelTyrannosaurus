import { mockTemplates } from '../../mocks'
import type { TemplateSummary } from '../../types/template'

export const ownerTemplateService = {
  async listTemplates(): Promise<TemplateSummary[]> {
    return mockTemplates.map((template) => ({ ...template }))
  },
}
