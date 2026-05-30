export type TemplateStatus = 'draft' | 'ready' | 'archived'

export interface TemplateSummary {
  id: string
  name: string
  version: string
  status: TemplateStatus
  fieldCount: number
  description: string
}
